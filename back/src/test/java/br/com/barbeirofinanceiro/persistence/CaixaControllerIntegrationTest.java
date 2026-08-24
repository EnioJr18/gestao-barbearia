package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CaixaControllerIntegrationTest extends PostgresPersistenceTest {
    private static final String USER_EMAIL = "caixa-user@teste.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CaixaRepository caixaRepository;

    @BeforeEach
    void criarUsuarioAutenticado() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário do Caixa");
        usuario.setEmail(USER_EMAIL);
        usuario.setSenhaHash("hash-de-teste");
        usuarioRepository.saveAndFlush(usuario);
    }

    @AfterEach
    void limparDados() {
        caixaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveAbrirCaixaComValorInicialValido() throws Exception {
        mockMvc.perform(post("/api/v1/caixas/abrir")
                        .with(csrf())
                        .with(user(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorInicial\":100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.valorInicial").value(100.0))
                .andExpect(jsonPath("$.valorEsperado").value(100.0))
                .andExpect(jsonPath("$.usuarioAberturaId").isNotEmpty())
                .andExpect(jsonPath("$.abertoEm").isNotEmpty());
    }

    @Test
    void deveRejeitarValorInicialInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/caixas/abrir")
                        .with(csrf())
                        .with(user(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorInicial\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void naoDeveAbrirSegundoCaixaEnquantoHouverUmAberto() throws Exception {
        abrir();

        mockMvc.perform(post("/api/v1/caixas/abrir")
                        .with(csrf())
                        .with(user(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorInicial\":50.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveConsultarCaixaAtual() throws Exception {
        abrir();

        mockMvc.perform(get("/api/v1/caixas/atual")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.valorEsperado").value(100.0));
    }

    @Test
    void deveRetornar404QuandoNaoHouverCaixaAtual() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/atual")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveFecharCaixaECalcularDiferenca() throws Exception {
        UUID id = abrir();

        mockMvc.perform(post("/api/v1/caixas/{id}/fechar", id)
                        .with(csrf())
                        .with(user(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorApurado\":320.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FECHADO"))
                .andExpect(jsonPath("$.valorApurado").value(320.0))
                .andExpect(jsonPath("$.valorEsperado").value(100.0))
                .andExpect(jsonPath("$.diferenca").value(220.0))
                .andExpect(jsonPath("$.usuarioFechamentoId").isNotEmpty())
                .andExpect(jsonPath("$.fechadoEm").isNotEmpty());
    }

    @Test
    void deveRejeitarValorApuradoNegativo() throws Exception {
        UUID id = abrir();

        mockMvc.perform(post("/api/v1/caixas/{id}/fechar", id)
                        .with(csrf())
                        .with(user(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorApurado\":-1.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void naoDeveFecharCaixaJaFechado() throws Exception {
        UUID id = abrir();
        fechar(id, 100.00);

        mockMvc.perform(post("/api/v1/caixas/{id}/fechar", id)
                        .with(csrf())
                        .with(user(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorApurado\":120.00}"))
                .andExpect(status().isConflict());
    }

    private UUID abrir() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/caixas/abrir")
                        .with(csrf())
                        .with(user(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorInicial\":100.00}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void fechar(UUID id, double valorApurado) throws Exception {
        mockMvc.perform(post("/api/v1/caixas/{id}/fechar", id)
                        .with(csrf())
                        .with(user(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorApurado\":" + valorApurado + "}"))
                .andExpect(status().isOk());
    }
}
