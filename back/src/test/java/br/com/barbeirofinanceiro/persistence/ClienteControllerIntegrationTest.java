package br.com.barbeirofinanceiro.persistence;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import br.com.barbeirofinanceiro.domain.cliente.ClienteRepository;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClienteControllerIntegrationTest extends PostgresPersistenceTest {
    private static final String USERNAME = "cliente-user@teste.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ClienteRepository clienteRepository;

    @AfterEach
    void limparDados() { clienteRepository.deleteAll(); }

    @Test
    void deveCriarClienteComNomeETelefone() throws Exception {
        mockMvc.perform(post("/api/v1/clientes").with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"João Silva\",\"telefone\":\"(82) 99999-9999\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.telefone").value("(82) 99999-9999"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveCriarClienteSemTelefone() throws Exception {
        mockMvc.perform(post("/api/v1/clientes").with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telefone").doesNotExist());
    }

    @Test
    void deveRejeitarNomeAusenteOuVazio() throws Exception {
        criarEsperandoStatus("{\"telefone\":\"999\"}", 400);
        criarEsperandoStatus("{\"nome\":\"   \"}", 400);
    }

    @Test
    void deveBuscarClientePorId() throws Exception {
        UUID id = criar("João", "999");

        mockMvc.perform(get("/api/v1/clientes/{id}", id).with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nome").value("João"));
    }

    @Test
    void deveListarEFiltrarPorNomeParcialCaseInsensitive() throws Exception {
        criar("João Silva", null);
        criar("Maria Souza", null);

        mockMvc.perform(get("/api/v1/clientes").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

        mockMvc.perform(get("/api/v1/clientes").param("nome", "SILV").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("João Silva"));
    }

    @Test
    void deveFiltrarPorAtivo() throws Exception {
        UUID ativo = criar("Ativo", null);
        UUID inativo = criar("Inativo", null);
        inativar(inativo);

        mockMvc.perform(get("/api/v1/clientes").param("ativo", "true").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ativo.toString()));
    }

    @Test
    void deveAtualizarCliente() throws Exception {
        UUID id = criar("João", "999");

        mockMvc.perform(put("/api/v1/clientes/{id}", id).with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"João Silva\",\"telefone\":\"888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.telefone").value("888"));
    }

    @Test
    void deveInativarEAtivarCliente() throws Exception {
        UUID id = criar("João", null);

        inativar(id);
        mockMvc.perform(get("/api/v1/clientes").param("ativo", "true").with(user(USERNAME)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(patch("/api/v1/clientes/{id}/ativar", id).with(csrf()).with(user(USERNAME)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void clienteInativoNaoApareceNoFiltroDeAtivos() throws Exception {
        UUID id = criar("João", null);
        inativar(id);

        mockMvc.perform(get("/api/v1/clientes").param("ativo", "true").with(user(USERNAME)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    private UUID criar(String nome, String telefone) throws Exception {
        String telefoneJson = telefone == null ? "" : ",\"telefone\":\"" + telefone + "\"";
        MvcResult result = mockMvc.perform(post("/api/v1/clientes").with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"" + telefoneJson + "}"))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void criarEsperandoStatus(String content, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/clientes").with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().is(expectedStatus));
    }

    private void inativar(UUID id) throws Exception {
        mockMvc.perform(patch("/api/v1/clientes/{id}/inativar", id).with(csrf()).with(user(USERNAME)))
                .andExpect(status().isOk());
    }
}
