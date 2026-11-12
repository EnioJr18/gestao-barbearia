package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.ItemRepository;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
    private ItemRepository itemRepository;

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
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/api/v1/itens").param("tipo", "PRODUTO").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nome").value("Pomada"));

        mockMvc.perform(get("/api/v1/itens").param("ativo", "false").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ativo").value(false));

        mockMvc.perform(get("/api/v1/itens").param("nome", "cort").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Corte"));
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
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void deveUsarPrimeiraPaginaComDezRegistrosPorPadrao() throws Exception {
        persistirItens("Item ", 12, TipoItem.SERVICO, true);

        mockMvc.perform(get("/api/v1/itens").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void deveConsultarPrimeiraESegundaPagina() throws Exception {
        persistirItens("Item ", 7, TipoItem.SERVICO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("page", "0").param("size", "3").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].nome").value("Item 00"))
                .andExpect(jsonPath("$.content[2].nome").value("Item 02"))
                .andExpect(jsonPath("$.page").value(0));

        mockMvc.perform(get("/api/v1/itens")
                        .param("page", "1").param("size", "3").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].nome").value("Item 03"))
                .andExpect(jsonPath("$.content[2].nome").value("Item 05"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.first").value(false));
    }

    @Test
    void deveInformarQuePaginaIntermediariaNaoEPrimeiraNemUltima() throws Exception {
        persistirItens("Item ", 10, TipoItem.SERVICO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("page", "1").param("size", "3").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(4))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void naoDeveRepetirIdsEntrePrimeiraESegundaPagina() throws Exception {
        persistirItens("Item ", 7, TipoItem.SERVICO, true);

        MvcResult primeiraPagina = mockMvc.perform(get("/api/v1/itens")
                        .param("page", "0").param("size", "3").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult segundaPagina = mockMvc.perform(get("/api/v1/itens")
                        .param("page", "1").param("size", "3").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(ids(primeiraPagina)).doesNotContainAnyElementsOf(ids(segundaPagina));
    }

    @Test
    void deveRetornarUltimaPaginaComQuantidadeInferiorAoTamanho() throws Exception {
        persistirItens("Item ", 13, TipoItem.SERVICO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("page", "1").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void deveAjustarPaginaInexistenteParaUltimaPaginaValida() throws Exception {
        persistirItens("Item ", 13, TipoItem.SERVICO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("page", "99").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(13))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void deveAjustarPaginaInexistenteConsiderandoOsFiltros() throws Exception {
        persistirItens("Tesoura ", 12, TipoItem.PRODUTO, true);
        persistirItens("Tesoura inativa ", 3, TipoItem.PRODUTO, false);
        persistirItens("Tesoura servico ", 3, TipoItem.SERVICO, true);
        persistirItens("Pomada ", 3, TipoItem.PRODUTO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("nome", "tesoura")
                        .param("tipo", "PRODUTO")
                        .param("ativo", "true")
                        .param("page", "99")
                        .param("size", "5")
                        .with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].tipo", everyItem(is("PRODUTO"))))
                .andExpect(jsonPath("$.content[*].ativo", everyItem(is(true))))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void deveRetornarPaginaVaziaQuandoNaoExistiremRegistros() throws Exception {
        mockMvc.perform(get("/api/v1/itens")
                        .param("page", "5").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void deveCombinarFiltroPorNomeComPaginacao() throws Exception {
        persistirItens("Tesoura ", 12, TipoItem.PRODUTO, true);
        persistirItens("Corte ", 2, TipoItem.SERVICO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("nome", "TESOURA").param("page", "1").param("size", "5")
                        .with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[*].nome", everyItem(org.hamcrest.Matchers.containsString("Tesoura"))))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void deveCombinarFiltroServicoComPaginacao() throws Exception {
        persistirItens("Servico ", 12, TipoItem.SERVICO, true);
        persistirItens("Produto ", 3, TipoItem.PRODUTO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("tipo", "SERVICO").param("page", "1").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].tipo", everyItem(is("SERVICO"))))
                .andExpect(jsonPath("$.totalElements").value(12));
    }

    @Test
    void deveCombinarFiltroProdutoComPaginacao() throws Exception {
        persistirItens("Produto ", 11, TipoItem.PRODUTO, true);
        persistirItens("Servico ", 3, TipoItem.SERVICO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("tipo", "PRODUTO").param("page", "1").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[*].tipo", everyItem(is("PRODUTO"))))
                .andExpect(jsonPath("$.totalElements").value(11));
    }

    @Test
    void deveCombinarFiltroAtivoComPaginacao() throws Exception {
        persistirItens("Ativo ", 4, TipoItem.SERVICO, true);
        persistirItens("Inativo ", 5, TipoItem.SERVICO, false);

        mockMvc.perform(get("/api/v1/itens")
                        .param("ativo", "false").param("page", "1").param("size", "3")
                        .with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].ativo", everyItem(is(false))))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void deveCombinarMultiplosFiltrosComPaginacao() throws Exception {
        persistirItens("Tesoura ", 7, TipoItem.PRODUTO, true);
        persistirItens("Tesoura inativa ", 2, TipoItem.PRODUTO, false);
        persistirItens("Tesoura servico ", 2, TipoItem.SERVICO, true);
        persistirItens("Pomada ", 2, TipoItem.PRODUTO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("nome", "tesoura")
                        .param("tipo", "PRODUTO")
                        .param("ativo", "true")
                        .param("page", "1")
                        .param("size", "4")
                        .with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].tipo", everyItem(is("PRODUTO"))))
                .andExpect(jsonPath("$.content[*].ativo", everyItem(is(true))))
                .andExpect(jsonPath("$.totalElements").value(7));
    }

    @Test
    void deveOrdenarPorNomeCrescente() throws Exception {
        persistirItem("Zulu", TipoItem.SERVICO, true);
        persistirItem("Alfa", TipoItem.SERVICO, true);
        persistirItem("Beta", TipoItem.SERVICO, true);

        mockMvc.perform(get("/api/v1/itens").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Alfa"))
                .andExpect(jsonPath("$.content[1].nome").value("Beta"))
                .andExpect(jsonPath("$.content[2].nome").value("Zulu"));
    }

    @Test
    void deveRejeitarParametrosDePaginacaoInvalidos() throws Exception {
        mockMvc.perform(get("/api/v1/itens").param("page", "-1").with(user(USERNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page deve ser maior ou igual a zero"));

        mockMvc.perform(get("/api/v1/itens").param("size", "0").with(user(USERNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size deve estar entre 1 e 100"));

        mockMvc.perform(get("/api/v1/itens").param("size", "-1").with(user(USERNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size deve estar entre 1 e 100"));

        mockMvc.perform(get("/api/v1/itens").param("size", "101").with(user(USERNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size deve estar entre 1 e 100"));
    }

    @Test
    void deveAceitarTamanhoMaximoDeCemRegistros() throws Exception {
        persistirItens("Item ", 12, TipoItem.SERVICO, true);

        mockMvc.perform(get("/api/v1/itens")
                        .param("size", "100").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(12)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void deveManterListagemProtegidaPorAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/itens"))
                .andExpect(status().isUnauthorized());
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

    private void persistirItens(String prefixo, int quantidade, TipoItem tipo, boolean ativo) {
        List<Item> itens = IntStream.range(0, quantidade)
                .mapToObj(indice -> novoItem(prefixo + "%02d".formatted(indice), tipo, ativo))
                .toList();
        itemRepository.saveAllAndFlush(itens);
    }

    private void persistirItem(String nome, TipoItem tipo, boolean ativo) {
        itemRepository.saveAndFlush(novoItem(nome, tipo, ativo));
    }

    private Item novoItem(String nome, TipoItem tipo, boolean ativo) {
        Item item = new Item();
        item.setNome(nome);
        item.setTipo(tipo);
        item.setPreco(BigDecimal.TEN);
        item.setEstoque(tipo == TipoItem.PRODUTO ? 10 : null);
        item.setAtivo(ativo);
        return item;
    }

    private Set<String> ids(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Set<String> ids = new HashSet<>();
        body.get("content").forEach(item -> ids.add(item.get("id").asText()));
        return ids;
    }
}
