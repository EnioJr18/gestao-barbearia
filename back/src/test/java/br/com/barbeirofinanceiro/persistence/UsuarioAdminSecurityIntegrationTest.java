package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.application.auth.JwtService;
import br.com.barbeirofinanceiro.application.usuario.UsuarioAdminLock;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioPerfil;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioAdminSecurityIntegrationTest extends PostgresPersistenceTest {

    private static final String ADMIN_EMAIL = "admin-seguranca@teste.local";
    private static final String OPERATOR_EMAIL = "operador-seguranca@teste.local";
    private static final String PASSWORD = "SenhaSegura123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioAdminLock usuarioAdminLock;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID adminId;
    private UUID operatorId;
    private String adminToken;
    private String operatorToken;

    @BeforeEach
    void prepararContas() {
        usuarios.deleteAll();
        adminId = criarUsuario("Admin", ADMIN_EMAIL, UsuarioPerfil.ADMIN);
        operatorId = criarUsuario("Operador", OPERATOR_EMAIL, UsuarioPerfil.OPERADOR);
        adminToken = token(ADMIN_EMAIL);
        operatorToken = token(OPERATOR_EMAIL);
    }

    @AfterEach
    void limparContas() {
        usuarios.deleteAll();
    }

    @Test
    void cadastroDeUsuarioExigeAdminEUsuarioNovoEOperadorPorPadrao() throws Exception {
        String body = """
                {"nome":"Novo operador","email":"novo-operador@teste.local","senha":"123456"}
                """;

        mockMvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", bearer(operatorToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("OPERADOR"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void operadorNaoPodeUsarEndpointsAdministrativos() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", bearer(operatorToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/usuarios/{id}", adminId).header("Authorization", bearer(operatorToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/usuarios/{id}", adminId)
                        .header("Authorization", bearer(operatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Alterado\",\"email\":\"alterado@teste.local\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/usuarios/{id}/desativar", adminId)
                        .header("Authorization", bearer(operatorToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPodeCriarPromoverAtivarDesativarEConsultarUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/usuarios/{id}", operatorId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("OPERADOR"));

        mockMvc.perform(put("/api/v1/usuarios/{id}", operatorId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Operador promovido\",\"email\":\"operador-seguranca@teste.local\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMIN"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro admin\",\"email\":\"outro-admin@teste.local\",\"senha\":\"123456\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("ADMIN"));

        mockMvc.perform(patch("/api/v1/usuarios/{id}/desativar", operatorId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/usuarios/{id}/ativar", operatorId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void atualizacaoPessoalRejeitaTentativaDeAlterarPerfil() throws Exception {
        mockMvc.perform(put("/api/v1/usuarios/me")
                        .header("Authorization", bearer(operatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Operador\",\"email\":\"operador-seguranca@teste.local\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        org.assertj.core.api.Assertions.assertThat(usuarios.findById(operatorId).orElseThrow().getPerfil())
                .isEqualTo(UsuarioPerfil.OPERADOR);
    }

    @Test
    void naoPermiteDesativarOuRebaixarUltimoAdmin() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/me/desativar")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/usuarios/{id}", adminId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Admin\",\"email\":\"admin-seguranca@teste.local\",\"perfil\":\"OPERADOR\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void desativacoesConcorrentesNaoRemovemTodosOsAdministradoresAtivos() throws Exception {
        UUID segundoAdminId = criarUsuario("Segundo admin", "segundo-admin@teste.local", UsuarioPerfil.ADMIN);
        String segundoAdminToken = token("segundo-admin@teste.local");
        CountDownLatch lockAdquirido = new CountDownLatch(1);
        CountDownLatch liberarLock = new CountDownLatch(1);
        CountDownLatch requisicoesIniciadas = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        AtomicReference<Throwable> erroHolder = new AtomicReference<>();
        Future<?> holder = executor.submit(() -> {
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    usuarioAdminLock.bloquearAlteracoesAdministrativas();
                    lockAdquirido.countDown();
                    try {
                        if (!liberarLock.await(15, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Tempo esgotado aguardando liberação do lock de teste");
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                });
            } catch (Throwable throwable) {
                erroHolder.set(throwable);
                lockAdquirido.countDown();
            }
        });
        try {
            org.assertj.core.api.Assertions.assertThat(lockAdquirido.await(10, TimeUnit.SECONDS)).isTrue();
            if (erroHolder.get() != null) {
                throw new AssertionError("Não foi possível adquirir o advisory lock de teste", erroHolder.get());
            }
            Future<Integer> primeira = executor.submit(() -> {
                requisicoesIniciadas.countDown();
                return mockMvc.perform(patch("/api/v1/usuarios/me/desativar")
                                .header("Authorization", bearer(adminToken)))
                        .andReturn().getResponse().getStatus();
            });
            Future<Integer> segunda = executor.submit(() -> {
                requisicoesIniciadas.countDown();
                return mockMvc.perform(patch("/api/v1/usuarios/me/desativar")
                                .header("Authorization", bearer(segundoAdminToken)))
                        .andReturn().getResponse().getStatus();
            });

            org.assertj.core.api.Assertions.assertThat(requisicoesIniciadas.await(10, TimeUnit.SECONDS)).isTrue();
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            Integer aguardando = 0;
            while (System.nanoTime() < limite) {
                aguardando = jdbcTemplate.queryForObject(
                        "select count(*) from pg_locks where locktype = 'advisory' and not granted",
                        Integer.class);
                if (aguardando != null && aguardando >= 2) {
                    break;
                }
                Thread.onSpinWait();
            }
            org.assertj.core.api.Assertions.assertThat(aguardando)
                    .as("as duas transações HTTP devem estar esperando o mesmo advisory lock PostgreSQL")
                    .isGreaterThanOrEqualTo(2);

            liberarLock.countDown();

            holder.get(10, TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat(java.util.List.of(
                            primeira.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(204, 409);
            org.assertj.core.api.Assertions.assertThat(usuarios.countByPerfilAndAtivoTrue(UsuarioPerfil.ADMIN))
                    .isEqualTo(1);
        } finally {
            liberarLock.countDown();
            executor.shutdownNow();
            org.assertj.core.api.Assertions.assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        org.assertj.core.api.Assertions.assertThat(usuarios.existsById(segundoAdminId)).isTrue();
    }

    @Test
    void tokenRefletePerfilAtualEDesativacaoImediata() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", bearer(operatorToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/usuarios/{id}", operatorId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Operador\",\"email\":\"operador-seguranca@teste.local\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", bearer(operatorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/usuarios/{id}", adminId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Admin\",\"email\":\"admin-seguranca@teste.local\",\"perfil\":\"OPERADOR\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", bearer(operatorToken)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/usuarios/{id}/desativar", operatorId)
                        .header("Authorization", bearer(operatorToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void operadorMantemAcessoAosRecursosOperacionais() throws Exception {
        for (String token : new String[]{adminToken, operatorToken}) {
            String authorization = bearer(token);
            MockHttpServletRequestBuilder[] requests = {
                    get("/api/v1/clientes"),
                    get("/api/v1/itens").param("page", "0").param("size", "10"),
                    get("/api/v1/vendas").param("page", "0").param("size", "10"),
                    get("/api/v1/caixas/historico").param("page", "0").param("size", "10"),
                    get("/api/v1/movimentacoes"),
                    get("/api/v1/dashboard"),
                    get("/api/v1/relatorios/resumo")
                            .param("dataInicial", "2026-01-01")
                            .param("dataFinal", "2026-12-31")
            };
            for (MockHttpServletRequestBuilder request : requests) {
                mockMvc.perform(request.header("Authorization", authorization))
                        .andExpect(status().is2xxSuccessful());
            }
        }
    }

    private UUID criarUsuario(String nome, String email, UsuarioPerfil perfil) {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(PASSWORD));
        usuario.setPerfil(perfil);
        return usuarios.saveAndFlush(usuario).getId();
    }

    private String token(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        return jwtService.gerarToken(User.withUsername(email)
                .password(usuario.getSenhaHash())
                .roles("USER")
                .build());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
