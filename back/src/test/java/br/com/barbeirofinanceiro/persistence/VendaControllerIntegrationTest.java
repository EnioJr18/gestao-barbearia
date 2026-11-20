package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.caixa.*;
import br.com.barbeirofinanceiro.domain.cliente.Cliente;
import br.com.barbeirofinanceiro.domain.cliente.ClienteRepository;
import br.com.barbeirofinanceiro.domain.item.*;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import br.com.barbeirofinanceiro.domain.venda.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VendaControllerIntegrationTest extends PostgresPersistenceTest {
    private static final String USER = "venda-user@teste.local";
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired CaixaRepository caixaRepository;
    @Autowired VendaRepository vendaRepository;
    @Autowired ItemVendaRepository itemVendaRepository;
    @Autowired VendaPagamentoRepository pagamentoRepository;

    @BeforeEach void preparar() {
        Usuario u = newEntity(Usuario.class); u.setNome("Vendedor"); u.setEmail(USER); u.setSenhaHash("hash"); usuarioRepository.saveAndFlush(u);
        Caixa c = new Caixa(); c.setDataCaixa(LocalDate.now()); c.setValorInicial(BigDecimal.valueOf(100)); c.setStatus(StatusCaixa.ABERTO); c.setUsuarioAbertura(u); caixaRepository.saveAndFlush(c);
    }
    @AfterEach void limpar() { pagamentoRepository.deleteAll(); itemVendaRepository.deleteAll(); vendaRepository.deleteAll(); caixaRepository.deleteAll(); itemRepository.deleteAll(); clienteRepository.deleteAll(); usuarioRepository.deleteAll(); }

    @Test void deveRegistrarVendaComServicoEPrecoHistorico() throws Exception {
        UUID item = item("Corte", TipoItem.SERVICO, "30", null);
        UUID id = criarVenda(item, 1, "PIX", "30");
        mockMvc.perform(get("/api/v1/vendas/{id}", id).with(user(USER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.valorTotal").value(30.0))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(30.0));
        assertThat(itemRepository.findById(item).get().getEstoque()).isNull();
    }

    @Test void deveBaixarEstoqueERegistrarVendaMista() throws Exception {
        UUID servico = item("Corte", TipoItem.SERVICO, "30", null);
        UUID produto = item("Pomada", TipoItem.PRODUTO, "35", 10);
        UUID cliente = cliente("João");
        String body = "{\"clienteId\":\"" + cliente + "\",\"itens\":[{\"itemId\":\"" + servico + "\",\"quantidade\":1},{\"itemId\":\"" + produto + "\",\"quantidade\":2}],\"pagamentos\":[{\"formaPagamento\":\"DINHEIRO\",\"valor\":100}]}";
        mockMvc.perform(post("/api/v1/vendas").with(csrf()).with(user(USER)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.valorTotal").value(100.0));
        assertThat(itemRepository.findById(produto).get().getEstoque()).isEqualTo(8);
    }

    @Test void deveAceitarPagamentoDividido() throws Exception {
        UUID item = item("Corte", TipoItem.SERVICO, "30", null);
        mockMvc.perform(post("/api/v1/vendas").with(csrf()).with(user(USER)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":1}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":10},{\"formaPagamento\":\"DINHEIRO\",\"valor\":20}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.pagamentos", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test void deveListarVendasComItensEPagamentos() throws Exception {
        UUID item = item("Corte", TipoItem.SERVICO, "30", null);
        criarVenda(item, 1, "PIX", "30");
        criarVenda(item, 1, "DINHEIRO", "30");

        mockMvc.perform(get("/api/v1/vendas").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].itens", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].pagamentos", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[1].itens", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[1].pagamentos", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test void devePaginarComMetadadosOrdenacaoDeterministicaESemRepeticao() throws Exception {
        UUID item = item("Corte paginado", TipoItem.SERVICO, "30", null);
        UUID vendaMaisRecente = null;
        for (int indice = 0; indice < 12; indice++) {
            UUID venda = persistirVenda(
                    LocalDate.of(2026, 10, 20).minusDays(indice),
                    null,
                    StatusVenda.FINALIZADA,
                    "30",
                    item
            );
            if (indice == 0) {
                vendaMaisRecente = venda;
            }
        }

        MvcResult primeira = mockMvc.perform(get("/api/v1/vendas").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(10)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content[0].id").value(vendaMaisRecente.toString()))
                .andReturn();

        MvcResult segunda = mockMvc.perform(get("/api/v1/vendas")
                        .param("page", "1")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true))
                .andReturn();

        Set<String> idsPrimeira = ids(primeira);
        Set<String> idsSegunda = ids(segunda);
        assertThat(idsPrimeira).doesNotContainAnyElementsOf(idsSegunda);
    }

    @Test void deveRetornarUltimaPaginaValidaQuandoPaginaNaoExiste() throws Exception {
        UUID item = item("Barba paginada", TipoItem.SERVICO, "20", null);
        for (int indice = 0; indice < 12; indice++) {
            persistirVenda(LocalDate.of(2026, 9, 20).minusDays(indice), null,
                    StatusVenda.FINALIZADA, "20", item);
        }

        mockMvc.perform(get("/api/v1/vendas")
                        .param("page", "99")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test void deveRetornarPaginaVaziaSemVendas() throws Exception {
        mockMvc.perform(get("/api/v1/vendas").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test void deveValidarParametrosDePaginacaoEAceitarLimiteMaximo() throws Exception {
        mockMvc.perform(get("/api/v1/vendas").param("page", "-1").with(user(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page deve ser maior ou igual a zero"));
        for (String size : new String[]{"0", "-1", "101"}) {
            mockMvc.perform(get("/api/v1/vendas").param("size", size).with(user(USER)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("size deve estar entre 1 e 100"));
        }
        mockMvc.perform(get("/api/v1/vendas").param("size", "100").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test void deveCombinarFiltrosExistentesComPaginacao() throws Exception {
        UUID item = item("Filtro de vendas", TipoItem.SERVICO, "40", null);
        UUID clienteAlvo = cliente("Cliente alvo");
        UUID outroCliente = cliente("Outro cliente");
        persistirVenda(LocalDate.of(2026, 8, 10), clienteAlvo,
                StatusVenda.FINALIZADA, "40", item);
        persistirVenda(LocalDate.of(2026, 8, 11), clienteAlvo,
                StatusVenda.FINALIZADA, "40", item);
        persistirVenda(LocalDate.of(2026, 8, 12), clienteAlvo,
                StatusVenda.CANCELADA, "40", item);
        persistirVenda(LocalDate.of(2026, 8, 11), outroCliente,
                StatusVenda.FINALIZADA, "40", item);

        mockMvc.perform(get("/api/v1/vendas")
                        .param("dataInicial", "2026-08-11")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get("/api/v1/vendas")
                        .param("dataFinal", "2026-08-10")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/vendas")
                        .param("dataInicial", "2026-08-10")
                        .param("dataFinal", "2026-08-11")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get("/api/v1/vendas")
                        .param("cliente", clienteAlvo.toString())
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get("/api/v1/vendas")
                        .param("status", "CANCELADA")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/vendas")
                        .param("dataInicial", "2026-08-10")
                        .param("dataFinal", "2026-08-11")
                        .param("cliente", clienteAlvo.toString())
                        .param("status", "FINALIZADA")
                        .param("page", "1")
                        .param("size", "1")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test void deveExigirAutenticacaoNaListagemPaginada() throws Exception {
        mockMvc.perform(get("/api/v1/vendas"))
                .andExpect(status().isUnauthorized());
    }

    @Test void deveRejeitarPagamentoDivergenteEQuantidadeInvalida() throws Exception {
        UUID item = item("Corte", TipoItem.SERVICO, "30", null);
        String base = "\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":%s}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":29}]";
        criarEsperando("{" + base.formatted("1") + "}", 400);
        criarEsperando("{" + base.formatted("0") + "}", 400);
    }

    @Test void deveRejeitarItemInativoEEstoqueInsuficiente() throws Exception {
        UUID item = item("Pomada", TipoItem.PRODUTO, "35", 1); Item produto = itemRepository.findById(item).get(); produto.setAtivo(false); itemRepository.saveAndFlush(produto);
        criarEsperando("{\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":1}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":35}]}", 409);
        produto.setAtivo(true); itemRepository.saveAndFlush(produto);
        criarEsperando("{\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":2}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":70}]}", 409);
    }

    @Test void devePreservarProdutoInativoEPrecoHistoricoAoEditarVenda() throws Exception {
        UUID item = item("Pomada", TipoItem.PRODUTO, "35", 5);
        UUID venda = criarVenda(item, 1, "PIX", "35");
        Item produto = itemRepository.findById(item).orElseThrow();
        produto.setAtivo(false);
        produto.setPreco(BigDecimal.valueOf(50));
        itemRepository.saveAndFlush(produto);

        String body = "{\"itens\":[{\"itemId\":\"" + item
                + "\",\"quantidade\":1}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":35}]}";
        mockMvc.perform(put("/api/v1/vendas/{id}", venda).with(csrf()).with(user(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(35.0))
                .andExpect(jsonPath("$.itens[0].itemId").value(item.toString()))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(35.0));

        mockMvc.perform(get("/api/v1/vendas/{id}", venda).with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.itens[0].itemId").value(item.toString()))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(35.0));
        assertThat(itemRepository.findById(item).orElseThrow().getEstoque()).isEqualTo(4);
    }

    @Test void deveAdicionarItemAtivoERemoverLinhaSomenteQuandoSolicitado() throws Exception {
        UUID produtoOriginal = item("Pomada", TipoItem.PRODUTO, "35", 5);
        UUID produtoNovo = item("Cera", TipoItem.PRODUTO, "20", 5);
        UUID venda = criarVenda(produtoOriginal, 1, "PIX", "35");

        String comNovoItem = "{\"itens\":[{\"itemId\":\"" + produtoOriginal
                + "\",\"quantidade\":1},{\"itemId\":\"" + produtoNovo
                + "\",\"quantidade\":1}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":55}]}";
        mockMvc.perform(put("/api/v1/vendas/{id}", venda).with(csrf()).with(user(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(comNovoItem))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", org.hamcrest.Matchers.hasSize(2)));

        String semItemOriginal = "{\"itens\":[{\"itemId\":\"" + produtoNovo
                + "\",\"quantidade\":1}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":20}]}";
        mockMvc.perform(put("/api/v1/vendas/{id}", venda).with(csrf()).with(user(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(semItemOriginal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.itens[0].itemId").value(produtoNovo.toString()));

        mockMvc.perform(get("/api/v1/vendas/{id}", venda).with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.itens[0].itemId").value(produtoNovo.toString()));
        assertThat(itemRepository.findById(produtoOriginal).orElseThrow().getEstoque()).isEqualTo(5);
        assertThat(itemRepository.findById(produtoNovo).orElseThrow().getEstoque()).isEqualTo(4);
    }

    @Test void naoDeveAdicionarItemInativoNovoDuranteEdicao() throws Exception {
        UUID itemOriginal = item("Corte", TipoItem.SERVICO, "30", null);
        UUID itemInativo = item("Barba", TipoItem.SERVICO, "20", null);
        UUID venda = criarVenda(itemOriginal, 1, "PIX", "30");
        Item inativo = itemRepository.findById(itemInativo).orElseThrow();
        inativo.setAtivo(false);
        itemRepository.saveAndFlush(inativo);

        String body = "{\"itens\":[{\"itemId\":\"" + itemOriginal
                + "\",\"quantidade\":1},{\"itemId\":\"" + itemInativo
                + "\",\"quantidade\":1}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":50}]}";
        mockMvc.perform(put("/api/v1/vendas/{id}", venda).with(csrf()).with(user(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/vendas/{id}", venda).with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.itens[0].itemId").value(itemOriginal.toString()));
    }

    @Test void deveCancelarPreservarHistoricoERestaurarEstoque() throws Exception {
        UUID item = item("Pomada", TipoItem.PRODUTO, "35", 2); UUID venda = criarVenda(item, 1, "PIX", "35");
        mockMvc.perform(post("/api/v1/vendas/{id}/cancelar", venda).with(csrf()).with(user(USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
        assertThat(itemRepository.findById(item).get().getEstoque()).isEqualTo(2);
        assertThat(vendaRepository.findById(venda)).isPresent();
        assertThat(itemVendaRepository.findByVendaId(venda)).hasSize(1);
        assertThat(pagamentoRepository.findByVendaId(venda)).hasSize(1);
    }

    @Test void naoDeveDuplicarReposicaoAoRepetirCancelamento() throws Exception {
        UUID item = item("Pomada", TipoItem.PRODUTO, "35", 2); UUID venda = criarVenda(item, 1, "PIX", "35");
        mockMvc.perform(post("/api/v1/vendas/{id}/cancelar", venda).with(csrf()).with(user(USER)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vendas/{id}/cancelar", venda).with(csrf()).with(user(USER)))
                .andExpect(status().isConflict());
        assertThat(itemRepository.findById(item).orElseThrow().getEstoque()).isEqualTo(2);
    }

    @Test void deleteNaoRemoveVendaNemHistorico() throws Exception {
        UUID item = item("Pomada", TipoItem.PRODUTO, "35", 2);
        UUID venda = criarVenda(item, 1, "PIX", "35");

        mockMvc.perform(delete("/api/v1/vendas/{id}", venda).with(csrf()).with(user(USER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cancelamento")));

        assertThat(vendaRepository.findById(venda)).isPresent();
        assertThat(vendaRepository.findById(venda).orElseThrow().getStatus()).isEqualTo(StatusVenda.FINALIZADA);
        assertThat(itemVendaRepository.findByVendaId(venda)).hasSize(1);
        assertThat(pagamentoRepository.findByVendaId(venda)).hasSize(1);
        assertThat(itemRepository.findById(item).orElseThrow().getEstoque()).isEqualTo(1);
    }

    @Test void itemComHistoricoNaoPodeSerExcluidoFisicamente() throws Exception {
        UUID item = item("Corte histórico", TipoItem.SERVICO, "30", null);
        criarVenda(item, 1, "PIX", "30");

        mockMvc.perform(delete("/api/v1/itens/{id}", item).with(csrf()).with(user(USER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Inative o item")));

        assertThat(itemRepository.findById(item)).isPresent();
    }

    @Test void clienteComHistoricoNaoPodeSerExcluidoEPreservaAgregados() throws Exception {
        UUID item = item("Corte cliente", TipoItem.SERVICO, "30", null);
        UUID cliente = cliente("Cliente histórico");
        criarVendaComCliente(item, cliente, "30");

        mockMvc.perform(delete("/api/v1/clientes/{id}", cliente).with(csrf()).with(user(USER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Inative o cliente")));

        mockMvc.perform(get("/api/v1/clientes/{id}", cliente).with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitas").value(1))
                .andExpect(jsonPath("$.totalGasto").value(30.0))
                .andExpect(jsonPath("$.ultimaVisita").value(LocalDate.now().toString()));
    }

    @Test void naoDeveCancelarVendaDeCaixaFechado() throws Exception {
        UUID item = item("Corte", TipoItem.SERVICO, "30", null);
        UUID venda = criarVenda(item, 1, "PIX", "30");
        Caixa caixa = caixaRepository.findFirstByStatusOrderByDataCaixaDesc(StatusCaixa.ABERTO).orElseThrow();

        mockMvc.perform(post("/api/v1/caixas/{id}/fechar", caixa.getId())
                        .with(user(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorApurado\":100.00}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/vendas/{id}/cancelar", venda).with(user(USER)))
                .andExpect(status().isConflict());
        assertThat(vendaRepository.findById(venda).orElseThrow().getStatus())
                .isEqualTo(StatusVenda.FINALIZADA);
    }

    @Test void deveSerializarCancelamentoComFechamentoDoCaixa() throws Exception {
        Caixa caixa = caixaRepository.findFirstByStatusOrderByDataCaixaDesc(StatusCaixa.ABERTO).orElseThrow();
        Venda venda = new Venda();
        venda.setCaixa(caixa);
        venda.setDataVenda(LocalDate.now());
        venda.setValorTotal(BigDecimal.valueOf(30));
        venda = vendaRepository.saveAndFlush(venda);
        VendaPagamento pagamento = new VendaPagamento();
        pagamento.setVenda(venda);
        pagamento.setFormaPagamento(br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento.DINHEIRO);
        pagamento.setValor(BigDecimal.valueOf(30));
        pagamentoRepository.saveAndFlush(pagamento);
        UUID vendaId = venda.getId();

        CountDownLatch iniciar = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> fechamento = executor.submit(() -> {
                iniciar.await();
                return mockMvc.perform(post("/api/v1/caixas/{id}/fechar", caixa.getId())
                                .with(user(USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"valorApurado\":100.00}"))
                        .andReturn().getResponse().getStatus();
            });
            Future<Integer> cancelamento = executor.submit(() -> {
                iniciar.await();
                return mockMvc.perform(post("/api/v1/vendas/{id}/cancelar", vendaId)
                                .with(user(USER)))
                        .andReturn().getResponse().getStatus();
            });
            iniciar.countDown();

            assertThat(fechamento.get()).isEqualTo(200);
            assertThat(cancelamento.get()).isIn(200, 409);
        }

        Venda vendaFinal = vendaRepository.findById(vendaId).orElseThrow();
        Caixa caixaFinal = caixaRepository.findById(caixa.getId()).orElseThrow();
        BigDecimal esperado = vendaFinal.getStatus() == StatusVenda.CANCELADA
                ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf(130);
        assertThat(caixaFinal.getDiferenca())
                .isEqualByComparingTo(BigDecimal.valueOf(100).subtract(esperado));
    }

    private UUID criarVenda(UUID item, int quantidade, String forma, String valor) throws Exception {
        String json = "{\"itens\":[{\"itemId\":\"" + item + "\",\"quantidade\":" + quantidade + "}],\"pagamentos\":[{\"formaPagamento\":\"" + forma + "\",\"valor\":" + valor + "}]}";
        MvcResult r = mockMvc.perform(post("/api/v1/vendas").with(csrf()).with(user(USER)).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isCreated()).andReturn();
        return UUID.fromString(mapper.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }
    private UUID criarVendaComCliente(UUID item, UUID cliente, String valor) throws Exception {
        String json = "{\"clienteId\":\"" + cliente + "\",\"itens\":[{\"itemId\":\"" + item
                + "\",\"quantidade\":1}],\"pagamentos\":[{\"formaPagamento\":\"PIX\",\"valor\":"
                + valor + "}]}";
        MvcResult result = mockMvc.perform(post("/api/v1/vendas").with(csrf()).with(user(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(mapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
    private UUID persistirVenda(LocalDate dataVenda, UUID clienteId, StatusVenda status,
                                String valor, UUID itemId) {
        Caixa caixa = caixaRepository.findFirstByStatusOrderByDataCaixaDesc(StatusCaixa.ABERTO)
                .orElseThrow();
        Venda venda = new Venda();
        venda.setCaixa(caixa);
        venda.setCliente(clienteId == null ? null : clienteRepository.findById(clienteId).orElseThrow());
        venda.setDataVenda(dataVenda);
        venda.setValorTotal(new BigDecimal(valor));
        venda.setStatus(status);
        venda = vendaRepository.saveAndFlush(venda);

        Item item = itemRepository.findById(itemId).orElseThrow();
        ItemVenda linha = new ItemVenda();
        linha.setVenda(venda);
        linha.setItem(item);
        linha.setQuantidade(1);
        linha.setPrecoUnitario(new BigDecimal(valor));
        linha.setSubtotal(new BigDecimal(valor));
        itemVendaRepository.saveAndFlush(linha);

        VendaPagamento pagamento = new VendaPagamento();
        pagamento.setVenda(venda);
        pagamento.setFormaPagamento(br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento.PIX);
        pagamento.setValor(new BigDecimal(valor));
        pagamentoRepository.saveAndFlush(pagamento);
        return venda.getId();
    }
    private Set<String> ids(MvcResult result) throws Exception {
        Set<String> ids = new HashSet<>();
        for (JsonNode node : mapper.readTree(result.getResponse().getContentAsString()).get("content")) {
            ids.add(node.get("id").asText());
        }
        return ids;
    }
    private void criarEsperando(String json, int code) throws Exception { mockMvc.perform(post("/api/v1/vendas").with(csrf()).with(user(USER)).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().is(code)); }
    private UUID item(String nome, TipoItem tipo, String preco, Integer estoque) { Item i=new Item(); i.setNome(nome); i.setTipo(tipo); i.setPreco(new BigDecimal(preco)); i.setEstoque(estoque); return itemRepository.saveAndFlush(i).getId(); }
    private UUID cliente(String nome) { Cliente c=newEntity(Cliente.class); c.setNome(nome); return clienteRepository.saveAndFlush(c).getId(); }
}
