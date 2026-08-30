package br.com.barbeirofinanceiro.persistence;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
class ItemControllerIntegrationTest extends PostgresPersistenceTest {
    private static final String USERNAME = "item-user@teste.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private br.com.barbeirofinanceiro.domain.item.ItemRepository itemRepository;

    @AfterEach
    void limparDados() {
        itemRepository.deleteAll();
    }

    @Test
    void deveCriarServico() throws Exception {
        mockMvc.perform(post("/api/v1/itens")
                        .with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Corte\",\"tipo\":\"SERVICO\",\"preco\":30.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Corte"))
                .andExpect(jsonPath("$.tipo").value("SERVICO"))
                .andExpect(jsonPath("$.estoque").doesNotExist())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveCriarProduto() throws Exception {
        mockMvc.perform(post("/api/v1/itens")
                        .with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pomada\",\"tipo\":\"PRODUTO\",\"preco\":35.00,\"estoque\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PRODUTO"))
                .andExpect(jsonPath("$.estoque").value(10));
    }

    @Test
    void deveRejeitarPrecoNegativo() throws Exception {
        criarEsperandoStatus("{\"nome\":\"Corte\",\"tipo\":\"SERVICO\",\"preco\":-1}", 400);
    }

    @Test
    void deveRejeitarEstoqueNegativo() throws Exception {
        criarEsperandoStatus("{\"nome\":\"Pomada\",\"tipo\":\"PRODUTO\",\"preco\":35,\"estoque\":-1}", 400);
    }

    @Test
    void deveRejeitarServicoComEstoque() throws Exception {
        criarEsperandoStatus("{\"nome\":\"Corte\",\"tipo\":\"SERVICO\",\"preco\":30,\"estoque\":1}", 400);
    }

    @Test
    void deveRejeitarProdutoSemEstoque() throws Exception {
        criarEsperandoStatus("{\"nome\":\"Pomada\",\"tipo\":\"PRODUTO\",\"preco\":35}", 400);
    }

    @Test
    void deveRejeitarNomeDuplicadoIgnorandoMaiusculas() throws Exception {
        criar("Corte", "SERVICO", "30", null);

        criarEsperandoStatus("{\"nome\":\"cOrTe\",\"tipo\":\"SERVICO\",\"preco\":30}", 409);
    }

    @Test
    void deveBuscarItemPorId() throws Exception {
        UUID id = criar("Corte", "SERVICO", "30", null);

        mockMvc.perform(get("/api/v1/itens/{id}", id).with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nome").value("Corte"));
    }

    @Test
    void deveListarEFiltrarItens() throws Exception {
        criar("Corte", "SERVICO", "30", null);
        UUID produtoId = criar("Pomada", "PRODUTO", "35", 10);
        inativar(produtoId);

        mockMvc.perform(get("/api/v1/itens").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

        mockMvc.perform(get("/api/v1/itens").param("tipo", "PRODUTO").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Pomada"));

        mockMvc.perform(get("/api/v1/itens").param("ativo", "false").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].ativo").value(false));

        mockMvc.perform(get("/api/v1/itens").param("nome", "cort").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Corte"));
    }

    @Test
    void deveAtualizarItemSemAlterarEstoque() throws Exception {
        UUID id = criar("Pomada", "PRODUTO", "35", 10);

        mockMvc.perform(put("/api/v1/itens/{id}", id)
                        .with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pomada Premium\",\"tipo\":\"PRODUTO\",\"preco\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pomada Premium"))
                .andExpect(jsonPath("$.preco").value(40.0))
                .andExpect(jsonPath("$.estoque").value(10));
    }

    @Test
    void deveInativarEAtivarItem() throws Exception {
        UUID id = criar("Corte", "SERVICO", "30", null);

        inativar(id);
        mockMvc.perform(get("/api/v1/itens/{id}", id).with(user(USERNAME)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(patch("/api/v1/itens/{id}/ativar", id)
                        .with(csrf()).with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void itemInativoNaoApareceNaListagemDeItensAtivos() throws Exception {
        UUID id = criar("Corte", "SERVICO", "30", null);
        inativar(id);

        mockMvc.perform(get("/api/v1/itens").param("ativo", "true").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private UUID criar(String nome, String tipo, String preco, Integer estoque) throws Exception {
        String estoqueJson = estoque == null ? "" : ",\"estoque\":" + estoque;
        MvcResult result = mockMvc.perform(post("/api/v1/itens")
                        .with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"tipo\":\"" + tipo
                                + "\",\"preco\":" + preco + estoqueJson + "}"))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void criarEsperandoStatus(String content, int status) throws Exception {
        mockMvc.perform(post("/api/v1/itens")
                        .with(csrf()).with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().is(status));
    }

    private void inativar(UUID id) throws Exception {
        mockMvc.perform(patch("/api/v1/itens/{id}/inativar", id)
                        .with(csrf()).with(user(USERNAME)))
                .andExpect(status().isOk());
    }
}
