package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.application.auth.JwtService;
import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.categoria.Categoria;
import br.com.barbeirofinanceiro.domain.categoria.CategoriaRepository;
import br.com.barbeirofinanceiro.domain.categoria.TipoCategoria;
import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.ItemRepository;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import br.com.barbeirofinanceiro.domain.movimentacao.Movimentacao;
import br.com.barbeirofinanceiro.domain.movimentacao.MovimentacaoRepository;
import br.com.barbeirofinanceiro.domain.movimentacao.OrigemMovimentacao;
import br.com.barbeirofinanceiro.domain.movimentacao.TipoMovimentacao;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import br.com.barbeirofinanceiro.domain.venda.ItemVenda;
import br.com.barbeirofinanceiro.domain.venda.ItemVendaRepository;
import br.com.barbeirofinanceiro.domain.venda.StatusVenda;
import br.com.barbeirofinanceiro.domain.venda.Venda;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamento;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamentoRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerIntegrationTest extends PostgresPersistenceTest {

    private static final String EMAIL = "dashboard@teste.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CaixaRepository caixaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private VendaRepository vendaRepository;
    @Autowired private ItemVendaRepository itemVendaRepository;
    @Autowired private VendaPagamentoRepository vendaPagamentoRepository;
    @Autowired private MovimentacaoRepository movimentacaoRepository;

    private Usuario usuario;
    private Caixa caixaFechado;
    private Categoria categoriaDespesa;
    private String token;

    @BeforeEach
    void preparar() {
        usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Dashboard");
        usuario.setEmail(EMAIL);
        usuario.setSenhaHash(passwordEncoder.encode("123456"));
        usuarioRepository.saveAndFlush(usuario);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(EMAIL)
                .password(usuario.getSenhaHash())
                .roles("USER")
                .build();
        token = jwtService.gerarToken(userDetails);

        caixaFechado = caixa(StatusCaixa.FECHADO, LocalDate.of(2026, 8, 1), BigDecimal.valueOf(50));
        categoriaDespesa = categoria("Operacional Dashboard", TipoCategoria.DESPESA);
    }

    @AfterEach
    void limpar() {
        vendaPagamentoRepository.deleteAll();
        itemVendaRepository.deleteAll();
        movimentacaoRepository.deleteAll();
        vendaRepository.deleteAll();
        caixaRepository.deleteAll();
        itemRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveRetornarDashboardVazioSemCaixaAtual() throws Exception {
        mockMvc.perform(dashboard("2026-08-10", "2026-08-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.dataInicial").value("2026-08-10"))
                .andExpect(jsonPath("$.resumo.faturamento").value(0))
                .andExpect(jsonPath("$.resumo.despesas").value(0))
                .andExpect(jsonPath("$.resumo.resultado").value(0))
                .andExpect(jsonPath("$.resumo.quantidadeVendas").value(0))
                .andExpect(jsonPath("$.caixaAtual").doesNotExist())
                .andExpect(jsonPath("$.itensMaisVendidos").isEmpty())
                .andExpect(jsonPath("$.evolucaoFaturamento").isEmpty());
    }

    @Test
    void deveCalcularResumoPagamentosItensEEvolucaoIgnorandoVendaCancelada() throws Exception {
        Item corte = item("Corte Dashboard", BigDecimal.valueOf(30));
        Item barba = item("Barba Dashboard", BigDecimal.valueOf(20));
        Venda vendaFinalizada = venda(caixaFechado, LocalDate.of(2026, 8, 10), BigDecimal.valueOf(50), StatusVenda.FINALIZADA);
        pagamento(vendaFinalizada, FormaPagamento.DINHEIRO, BigDecimal.valueOf(30));
        pagamento(vendaFinalizada, FormaPagamento.PIX, BigDecimal.valueOf(20));
        itemVenda(vendaFinalizada, corte, 1, 30);
        itemVenda(vendaFinalizada, barba, 1, 20);

        Venda cancelada = venda(caixaFechado, LocalDate.of(2026, 8, 11), BigDecimal.valueOf(99), StatusVenda.CANCELADA);
        pagamento(cancelada, FormaPagamento.CARTAO_CREDITO, BigDecimal.valueOf(99));
        itemVenda(cancelada, corte, 3, 99);

        despesa(LocalDate.of(2026, 8, 10), BigDecimal.valueOf(10));

        mockMvc.perform(dashboard("2026-08-10", "2026-08-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.faturamento").value(50))
                .andExpect(jsonPath("$.resumo.despesas").value(10))
                .andExpect(jsonPath("$.resumo.resultado").value(40))
                .andExpect(jsonPath("$.resumo.quantidadeVendas").value(1))
                .andExpect(jsonPath("$.pagamentos.dinheiro").value(30))
                .andExpect(jsonPath("$.pagamentos.pix").value(20))
                .andExpect(jsonPath("$.pagamentos.cartaoCredito").value(0))
                .andExpect(jsonPath("$.itensMaisVendidos[0].nome").value("Corte Dashboard"))
                .andExpect(jsonPath("$.itensMaisVendidos[0].faturamento").value(30))
                .andExpect(jsonPath("$.evolucaoFaturamento[0].data").value("2026-08-10"))
                .andExpect(jsonPath("$.evolucaoFaturamento[0].faturamento").value(50));
    }

    @Test
    void deveExcluirDespesaForaDoPeriodo() throws Exception {
        despesa(LocalDate.of(2026, 8, 9), BigDecimal.valueOf(10));
        despesa(LocalDate.of(2026, 8, 10), BigDecimal.valueOf(25));

        mockMvc.perform(dashboard("2026-08-10", "2026-08-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.despesas").value(25));
    }

    @Test
    void deveLimitarTopCincoEOrdenarPorFaturamento() throws Exception {
        for (int numero = 1; numero <= 6; numero++) {
            Item item = item("Item Top " + numero, BigDecimal.valueOf(numero * 10L));
            Venda venda = venda(caixaFechado, LocalDate.of(2026, 8, 12), BigDecimal.valueOf(numero * 10L), StatusVenda.FINALIZADA);
            itemVenda(venda, item, 1, numero * 10);
        }

        mockMvc.perform(dashboard("2026-08-12", "2026-08-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itensMaisVendidos.length()").value(5))
                .andExpect(jsonPath("$.itensMaisVendidos[0].nome").value("Item Top 6"))
                .andExpect(jsonPath("$.itensMaisVendidos[0].faturamento").value(60))
                .andExpect(jsonPath("$.itensMaisVendidos[4].nome").value("Item Top 2"));
    }

    @Test
    void deveRetornarCaixaAtualComRegraFinanceiraExistente() throws Exception {
        Caixa caixaAberto = caixa(StatusCaixa.ABERTO, LocalDate.of(2026, 8, 2), BigDecimal.valueOf(100));
        Venda venda = venda(caixaAberto, LocalDate.of(2026, 8, 12), BigDecimal.valueOf(40), StatusVenda.FINALIZADA);
        pagamento(venda, FormaPagamento.DINHEIRO, BigDecimal.valueOf(40));
        Movimentacao movimentacao = despesa(LocalDate.of(2026, 8, 12), BigDecimal.valueOf(15));
        movimentacao.setCaixa(caixaAberto);
        movimentacaoRepository.saveAndFlush(movimentacao);

        mockMvc.perform(dashboard("2026-08-12", "2026-08-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caixaAtual.status").value("ABERTO"))
                .andExpect(jsonPath("$.caixaAtual.valorInicial").value(100))
                .andExpect(jsonPath("$.caixaAtual.entradasDinheiro").value(40))
                .andExpect(jsonPath("$.caixaAtual.saidasDinheiro").value(15))
                .andExpect(jsonPath("$.caixaAtual.valorEsperado").value(125));
    }

    @Test
    void deveUsarMesAtualQuandoPeriodoForOmitido() throws Exception {
        YearMonth mesAtual = YearMonth.now();

        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.dataInicial").value(mesAtual.atDay(1).toString()))
                .andExpect(jsonPath("$.periodo.dataFinal").value(mesAtual.atEndOfMonth().toString()));
    }

    @Test
    void deveRejeitarPeriodoIncompletoOuInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").param("dataInicial", "2026-08-10").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/dashboard").param("dataFinal", "2026-08-10").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(dashboard("2026-08-11", "2026-08-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A data inicial não pode ser posterior à data final"));
    }

    @Test
    void deveExigirJwtValido() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder dashboard(
            String dataInicial,
            String dataFinal
    ) {
        return get("/api/v1/dashboard")
                .param("dataInicial", dataInicial)
                .param("dataFinal", dataFinal)
                .header("Authorization", bearer());
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private Caixa caixa(StatusCaixa status, LocalDate data, BigDecimal valorInicial) {
        Caixa caixa = new Caixa();
        caixa.setDataCaixa(data);
        caixa.setValorInicial(valorInicial);
        caixa.setStatus(status);
        caixa.setUsuarioAbertura(usuario);
        if (status == StatusCaixa.FECHADO) {
            caixa.setValorApurado(valorInicial);
            caixa.setDiferenca(BigDecimal.ZERO);
            caixa.setUsuarioFechamento(usuario);
            caixa.setFechadoEm(Instant.now().plusSeconds(1));
        }
        return caixaRepository.saveAndFlush(caixa);
    }

    private Categoria categoria(String nome, TipoCategoria tipo) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome + System.nanoTime());
        categoria.setTipo(tipo);
        return categoriaRepository.saveAndFlush(categoria);
    }

    private Item item(String nome, BigDecimal preco) {
        Item item = new Item();
        item.setNome(nome);
        item.setTipo(TipoItem.SERVICO);
        item.setPreco(preco);
        item.setEstoque(null);
        return itemRepository.saveAndFlush(item);
    }

    private Venda venda(Caixa caixa, LocalDate data, BigDecimal valor, StatusVenda status) {
        Venda venda = newEntity(Venda.class);
        venda.setCaixa(caixa);
        venda.setDataVenda(data);
        venda.setValorTotal(valor);
        venda.setStatus(status);
        return vendaRepository.saveAndFlush(venda);
    }

    private void pagamento(Venda venda, FormaPagamento formaPagamento, BigDecimal valor) {
        VendaPagamento pagamento = new VendaPagamento();
        pagamento.setVenda(venda);
        pagamento.setFormaPagamento(formaPagamento);
        pagamento.setValor(valor);
        vendaPagamentoRepository.saveAndFlush(pagamento);
    }

    private void itemVenda(Venda venda, Item item, int quantidade, int subtotal) {
        ItemVenda itemVenda = new ItemVenda();
        itemVenda.setVenda(venda);
        itemVenda.setItem(item);
        itemVenda.setQuantidade(quantidade);
        itemVenda.setPrecoUnitario(BigDecimal.valueOf(subtotal).divide(BigDecimal.valueOf(quantidade)));
        itemVenda.setSubtotal(BigDecimal.valueOf(subtotal));
        itemVendaRepository.saveAndFlush(itemVenda);
    }

    private Movimentacao despesa(LocalDate data, BigDecimal valor) {
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setTipo(TipoMovimentacao.DESPESA);
        movimentacao.setOrigem(OrigemMovimentacao.MANUAL);
        movimentacao.setDescricao("Despesa Dashboard");
        movimentacao.setValor(valor);
        movimentacao.setDataMovimentacao(data);
        movimentacao.setCategoria(categoriaDespesa);
        movimentacao.setFormaPagamento(FormaPagamento.DINHEIRO);
        return movimentacaoRepository.saveAndFlush(movimentacao);
    }
}
