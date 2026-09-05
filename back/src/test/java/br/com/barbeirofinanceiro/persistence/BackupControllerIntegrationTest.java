package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.application.auth.JwtService;
import br.com.barbeirofinanceiro.application.backup.BackupExecutionException;
import br.com.barbeirofinanceiro.application.backup.ManutencaoCoordinator;
import br.com.barbeirofinanceiro.application.backup.PostgresBackupExecutor;
import br.com.barbeirofinanceiro.domain.backup.BackupExecucaoRepository;
import br.com.barbeirofinanceiro.domain.backup.BackupStatus;
import br.com.barbeirofinanceiro.domain.backup.BackupTipo;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BackupControllerIntegrationTest extends PostgresPersistenceTest {

    private static final Path BACKUP_DIRECTORY = criarDiretorioTemporario();
    private static final String EMAIL = "backup@teste.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BackupExecucaoRepository backupExecucaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ManutencaoCoordinator manutencaoCoordinator;

    @MockitoBean
    private PostgresBackupExecutor postgresBackupExecutor;

    private String token;

    @DynamicPropertySource
    static void configurarBackup(DynamicPropertyRegistry registry) {
        registry.add("backup.diretorio", () -> BACKUP_DIRECTORY.toString());
    }

    @BeforeEach
    void prepararUsuario() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Backup");
        usuario.setEmail(EMAIL);
        usuario.setSenhaHash(passwordEncoder.encode("123456"));
        usuarioRepository.saveAndFlush(usuario);
        token = jwtService.gerarToken(User.withUsername(EMAIL)
                .password(usuario.getSenhaHash())
                .roles("USER")
                .build());
    }

    @AfterEach
    void limparDados() throws Exception {
        backupExecucaoRepository.deleteAll();
        usuarioRepository.deleteAll();
        try (var arquivos = Files.walk(BACKUP_DIRECTORY)) {
            arquivos.sorted(java.util.Comparator.reverseOrder())
                    .filter(path -> !path.equals(BACKUP_DIRECTORY))
                    .forEach(this::excluir);
        }
    }

    @AfterAll
    static void removerDiretorioTemporario() throws Exception {
        try (var arquivos = Files.walk(BACKUP_DIRECTORY)) {
            arquivos.sorted(java.util.Comparator.reverseOrder()).forEach(BackupControllerIntegrationTest::excluirEstatico);
        }
    }

    @Test
    void deveExigirAutenticacaoParaBackups() throws Exception {
        mockMvc.perform(post("/api/v1/backups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarJwtInvalidoParaBackups() throws Exception {
        mockMvc.perform(get("/api/v1/backups").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveCriarArquivoERegistrarSucesso() throws Exception {
        doAnswer(invocation -> {
            Files.writeString(invocation.getArgument(0, Path.class), "backup de teste");
            return null;
        }).when(postgresBackupExecutor).criarBackup(any(Path.class));

        mockMvc.perform(post("/api/v1/backups").header("Authorization", bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.arquivo").value(org.hamcrest.Matchers.endsWith(".backup")))
                .andExpect(jsonPath("$.status").value("SUCESSO"));

        org.junit.jupiter.api.Assertions.assertEquals(1, backupExecucaoRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(BackupStatus.SUCESSO,
                backupExecucaoRepository.findAll().getFirst().getStatus());
        verify(postgresBackupExecutor).validarBackup(any(Path.class));
    }

    @Test
    void deveRegistrarFalhaDoUtilitario() throws Exception {
        doThrow(new BackupExecutionException("Falha ao gerar o backup"))
                .when(postgresBackupExecutor).criarBackup(any(Path.class));

        mockMvc.perform(post("/api/v1/backups").header("Authorization", bearer()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Falha ao gerar o backup"));

        org.junit.jupiter.api.Assertions.assertEquals(BackupStatus.FALHA,
                backupExecucaoRepository.findAll().getFirst().getStatus());
    }

    @Test
    void deveListarSomenteArquivosDisponiveis() throws Exception {
        criarArquivoBackup();

        mockMvc.perform(get("/api/v1/backups").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].arquivo").value(org.hamcrest.Matchers.endsWith(".backup")))
                .andExpect(jsonPath("$[0].status").value("SUCESSO"));
    }

    @Test
    void deveRejeitarIdentificadorComPathTraversalNaRestauracao() throws Exception {
        mockMvc.perform(post("/api/v1/backups/..%2Fsegredo.backup/restaurar")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void naoDeveRestaurarArquivoValidoNaoRegistradoPeloSistema() throws Exception {
        String arquivo = criarArquivoSemRegistro();

        mockMvc.perform(post("/api/v1/backups/{arquivo}/restaurar", arquivo)
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void deveRestaurarArquivoValidoEInformarReinicioNecessario() throws Exception {
        String arquivo = criarArquivoBackup();

        mockMvc.perform(post("/api/v1/backups/{arquivo}/restaurar", arquivo)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arquivo").value(arquivo))
                .andExpect(jsonPath("$.reinicioNecessario").value(true));

        verify(postgresBackupExecutor).validarBackup(any(Path.class));
        verify(postgresBackupExecutor).restaurarBackup(any(Path.class));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void deveManterAplicacaoIndisponivelAposRestauracaoAteReinicio() throws Exception {
        String arquivo = criarArquivoBackup();

        mockMvc.perform(post("/api/v1/backups/{arquivo}/restaurar", arquivo)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reinicioNecessario").value(true));

        mockMvc.perform(get("/api/v1/backups").header("Authorization", bearer()))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(post("/api/v1/caixas/abrir")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"valorInicial\":100.00}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void deveBloquearEscritasEnquantoHaRestauracao() throws Exception {
        Thread restauracao = new Thread(() -> manutencaoCoordinator.executarRestauracao(() -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return null;
        }));
        restauracao.start();
        Thread.sleep(100);

        mockMvc.perform(post("/api/v1/caixas/abrir")
                .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"valorInicial\":100.00}"))
                .andExpect(status().isServiceUnavailable());

        restauracao.join();
    }

    private String criarArquivoBackup() throws Exception {
        String arquivo = criarArquivoSemRegistro();
        var execucao = br.com.barbeirofinanceiro.domain.backup.BackupExecucao.iniciar(BackupTipo.MANUAL, arquivo);
        execucao.setStatus(BackupStatus.SUCESSO);
        backupExecucaoRepository.saveAndFlush(execucao);
        return arquivo;
    }

    private String criarArquivoSemRegistro() throws Exception {
        String arquivo = "backup-20260903T000000Z-123e4567-e89b-12d3-a456-426614174000.backup";
        Files.writeString(BACKUP_DIRECTORY.resolve(arquivo), "backup de teste");
        return arquivo;
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private void excluir(Path path) {
        excluirEstatico(path);
    }

    private static Path criarDiretorioTemporario() {
        try {
            return Files.createTempDirectory("barbeiro-backup-test-");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void excluirEstatico(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
