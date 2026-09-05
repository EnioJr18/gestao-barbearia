package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerIntegrationTest extends PostgresPersistenceTest {

    private static final String EMAIL_USUARIO_A = "usuario-a@teste.local";
    private static final String EMAIL_USUARIO_B = "usuario-b@teste.local";
    private static final String SENHA_ATUAL = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID usuarioAId;
    private UUID usuarioBId;

    @BeforeEach
    void prepararUsuarios() {
        usuarioAId = criarUsuario("Usuário A", EMAIL_USUARIO_A, SENHA_ATUAL);
        usuarioBId = criarUsuario("Usuário B", EMAIL_USUARIO_B, SENHA_ATUAL);
    }

    @AfterEach
    void limparUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void deveConsultarApenasOUsuarioAutenticadoSemExporSenhaHash() throws Exception {
        String token = login(EMAIL_USUARIO_A, SENHA_ATUAL);

        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioAId.toString()))
                .andExpect(jsonPath("$.nome").value("Usuário A"))
                .andExpect(jsonPath("$.email").value(EMAIL_USUARIO_A))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void deveRetornarUnauthorizedSemJwtOuComJwtInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveAtualizarNomeEEmailEInvalidarTokenComEmailAnterior() throws Exception {
        String tokenAnterior = login(EMAIL_USUARIO_A, SENHA_ATUAL);

        mockMvc.perform(put("/api/v1/usuarios/me")
                        .header("Authorization", bearer(tokenAnterior))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nome": "Usuário Atualizado",
                              "email": "novo-email@teste.local"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioAId.toString()))
                .andExpect(jsonPath("$.nome").value("Usuário Atualizado"))
                .andExpect(jsonPath("$.email").value("novo-email@teste.local"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", bearer(tokenAnterior)))
                .andExpect(status().isUnauthorized());

        Assertions.assertThat(login("novo-email@teste.local", SENHA_ATUAL)).isNotBlank();
    }

    @Test
    void deveRejeitarEmailInvalido() throws Exception {
        String token = login(EMAIL_USUARIO_A, SENHA_ATUAL);

        mockMvc.perform(put("/api/v1/usuarios/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "nome": "Usuário A",
                              "email": "email-invalido"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarEmailDuplicado() throws Exception {
        String token = login(EMAIL_USUARIO_A, SENHA_ATUAL);

        mockMvc.perform(put("/api/v1/usuarios/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(atualizarUsuario("Usuário A", EMAIL_USUARIO_B)))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRejeitarEmailDuplicadoIgnorandoMaiusculasEMinusculas() throws Exception {
        String token = login(EMAIL_USUARIO_A, SENHA_ATUAL);

        mockMvc.perform(put("/api/v1/usuarios/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(atualizarUsuario("Usuário A", EMAIL_USUARIO_B.toUpperCase())))
                .andExpect(status().isConflict());
    }

    @Test
    void deveAlterarSenhaComBcryptEInvalidarTokenAnterior() throws Exception {
        String tokenAnterior = login(EMAIL_USUARIO_A, SENHA_ATUAL);

        mockMvc.perform(put("/api/v1/usuarios/me/senha")
                        .header("Authorization", bearer(tokenAnterior))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "senhaAtual": "123456",
                              "novaSenha": "nova-senha"
                            }
                            """))
                .andExpect(status().isNoContent());

        Usuario usuario = usuarioRepository.findById(usuarioAId).orElseThrow();
        Assertions.assertThat(usuario.getSenhaHash()).startsWith("$2");
        Assertions.assertThat(passwordEncoder.matches("nova-senha", usuario.getSenhaHash())).isTrue();

        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", bearer(tokenAnterior)))
                .andExpect(status().isUnauthorized());

        loginEsperandoStatus(EMAIL_USUARIO_A, SENHA_ATUAL, 401);
        Assertions.assertThat(login(EMAIL_USUARIO_A, "nova-senha")).isNotBlank();
    }

    @Test
    void deveRejeitarAlteracaoQuandoSenhaAtualForIncorreta() throws Exception {
        String token = login(EMAIL_USUARIO_A, SENHA_ATUAL);

        mockMvc.perform(put("/api/v1/usuarios/me/senha")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "senhaAtual": "senha-incorreta",
                              "novaSenha": "nova-senha"
                            }
                            """))
                .andExpect(status().isBadRequest());

        Assertions.assertThat(login(EMAIL_USUARIO_A, SENHA_ATUAL)).isNotBlank();
    }

    @Test
    void deveDesativarUsuarioEInvalidarTokenAtual() throws Exception {
        String token = login(EMAIL_USUARIO_A, SENHA_ATUAL);

        mockMvc.perform(patch("/api/v1/usuarios/me/desativar")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        Assertions.assertThat(usuarioRepository.findById(usuarioAId).orElseThrow().isAtivo()).isFalse();

        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());

        loginEsperandoStatus(EMAIL_USUARIO_A, SENHA_ATUAL, 401);
    }

    @Test
    void tokenDoUsuarioANaoPermiteAcessarDadosDoUsuarioB() throws Exception {
        String tokenUsuarioA = login(EMAIL_USUARIO_A, SENHA_ATUAL);

        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", bearer(tokenUsuarioA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioAId.toString()))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(usuarioBId.toString())))
                .andExpect(jsonPath("$.email").value(EMAIL_USUARIO_A));
    }

    private UUID criarUsuario(String nome, String email, String senha) {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        return usuarioRepository.saveAndFlush(usuario).getId();
    }

    private String login(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private void loginEsperandoStatus(String email, String senha, int statusEsperado) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().is(statusEsperado));
    }

    private String atualizarUsuario(String nome, String email) {
        return "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\"}";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
