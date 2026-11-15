package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.application.caixa.CaixaResponse;
import br.com.barbeirofinanceiro.application.caixa.CaixaService;
import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.categoria.Categoria;
import br.com.barbeirofinanceiro.domain.categoria.CategoriaRepository;
import br.com.barbeirofinanceiro.domain.categoria.TipoCategoria;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import br.com.barbeirofinanceiro.domain.movimentacao.Movimentacao;
import br.com.barbeirofinanceiro.domain.movimentacao.MovimentacaoRepository;
import br.com.barbeirofinanceiro.domain.movimentacao.OrigemMovimentacao;
import br.com.barbeirofinanceiro.domain.movimentacao.TipoMovimentacao;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import br.com.barbeirofinanceiro.domain.venda.StatusVenda;
import br.com.barbeirofinanceiro.domain.venda.Venda;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamento;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamentoRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class CaixaHistoricoIntegrationTest extends PostgresPersistenceTest {

    private static final String USER_EMAIL = "historico-caixa@teste.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private CaixaService caixaService;
    @Autowired private CaixaRepository caixaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VendaRepository vendaRepository;
    @Autowired private VendaPagamentoRepository pagamentoRepository;
    @Autowired private MovimentacaoRepository movimentacaoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Usuario usuario;
    private Categoria categoriaDespesa;

    @BeforeEach
    void preparar() {
        usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Histórico");
        usuario.setEmail(USER_EMAIL);
        usuario.setSenhaHash("hash");
        usuarioRepository.saveAndFlush(usuario);

        categoriaDespesa = newEntity(Categoria.class);
        categoriaDespesa.setNome("Despesa Histórico");
        categoriaDespesa.setTipo(TipoCategoria.DESPESA);
        categoriaRepository.saveAndFlush(categoriaDespesa);
    }

    @AfterEach
    void limpar() {
        pagamentoRepository.deleteAll();
        movimentacaoRepository.deleteAll();
        vendaRepository.deleteAll();
        caixaRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveRetornarHistoricoVazio() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/historico").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deveManterContratoParaCaixaSemMovimentacoes() throws Exception {
        Caixa caixa = caixa(LocalDate.of(2026, 8, 1), StatusCaixa.ABERTO, "100", null, null);

        mockMvc.perform(get("/api/v1/caixas/historico").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(caixa.getId().toString()))
                .andExpect(jsonPath("$[0].dataCaixa").value("2026-08-01"))
                .andExpect(jsonPath("$[0].valorInicial").value(100))
                .andExpect(jsonPath("$[0].entradasDinheiro").value(0))
                .andExpect(jsonPath("$[0].saidasDinheiro").value(0))
                .andExpect(jsonPath("$[0].dinheiro").value(0))
                .andExpect(jsonPath("$[0].pix").value(0))
                .andExpect(jsonPath("$[0].debito").value(0))
                .andExpect(jsonPath("$[0].credito").value(0))
                .andExpect(jsonPath("$[0].valorEsperado").value(100))
                .andExpect(jsonPath("$[0].status").value("ABERTO"))
                .andExpect(jsonPath("$[0].usuarioAberturaId").value(usuario.getId().toString()))
                .andExpect(jsonPath("$[0].abertoEm").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[0].updatedAt").isNotEmpty());
    }

    @Test
    void deveAgregarFormasDespesasEIgnorarVendaCancelada() throws Exception {
        Caixa caixa = caixa(LocalDate.of(2026, 8, 2), StatusCaixa.ABERTO, "100", null, null);
        Venda finalizada = venda(caixa, StatusVenda.FINALIZADA, "100");
        pagamento(finalizada, FormaPagamento.DINHEIRO, "10");
        pagamento(finalizada, FormaPagamento.PIX, "20");
        pagamento(finalizada, FormaPagamento.CARTAO_CREDITO, "30");
        pagamento(finalizada, FormaPagamento.CARTAO_DEBITO, "40");

        Venda cancelada = venda(caixa, StatusVenda.CANCELADA, "50");
        pagamento(cancelada, FormaPagamento.DINHEIRO, "50");
        movimentacao(caixa, TipoMovimentacao.DESPESA, FormaPagamento.DINHEIRO, "15");
        movimentacao(caixa, TipoMovimentacao.DESPESA, FormaPagamento.PIX, "7");
        movimentacao(caixa, TipoMovimentacao.RECEITA, FormaPagamento.DINHEIRO, "9");

        mockMvc.perform(get("/api/v1/caixas/historico").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entradasDinheiro").value(10))
                .andExpect(jsonPath("$[0].saidasDinheiro").value(15))
                .andExpect(jsonPath("$[0].dinheiro").value(10))
                .andExpect(jsonPath("$[0].pix").value(20))
                .andExpect(jsonPath("$[0].credito").value(30))
                .andExpect(jsonPath("$[0].debito").value(40))
                .andExpect(jsonPath("$[0].valorEsperado").value(95));
    }

    @Test
    void deveSepararValoresPorCaixaEPreservarOrdenacao() throws Exception {
        Caixa antigo = caixa(LocalDate.of(2026, 8, 1), StatusCaixa.FECHADO, "100", "110", "0");
        Caixa recente = caixa(LocalDate.of(2026, 8, 2), StatusCaixa.FECHADO, "200", "200", "5");
        Venda vendaAntiga = venda(antigo, StatusVenda.FINALIZADA, "11");
        pagamento(vendaAntiga, FormaPagamento.DINHEIRO, "11");
        movimentacao(antigo, TipoMovimentacao.DESPESA, FormaPagamento.DINHEIRO, "1");
        Venda vendaRecente = venda(recente, StatusVenda.FINALIZADA, "22");
        pagamento(vendaRecente, FormaPagamento.PIX, "22");
        movimentacao(recente, TipoMovimentacao.DESPESA, FormaPagamento.DINHEIRO, "2");

        mockMvc.perform(get("/api/v1/caixas/historico").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(recente.getId().toString()))
                .andExpect(jsonPath("$[0].dinheiro").value(0))
                .andExpect(jsonPath("$[0].pix").value(22))
                .andExpect(jsonPath("$[0].saidasDinheiro").value(2))
                .andExpect(jsonPath("$[0].valorEsperado").value(195))
                .andExpect(jsonPath("$[1].id").value(antigo.getId().toString()))
                .andExpect(jsonPath("$[1].dinheiro").value(11))
                .andExpect(jsonPath("$[1].pix").value(0))
                .andExpect(jsonPath("$[1].saidasDinheiro").value(1))
                .andExpect(jsonPath("$[1].valorEsperado").value(110));
    }

    @Test
    void quantidadeDeConsultasPermaneceConstanteComMaisCaixas() {
        caixa(LocalDate.of(2026, 8, 1), StatusCaixa.FECHADO, "100", "100", "0");
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<CaixaResponse> umCaixa = caixaService.listarComResposta();
        long consultasComUmCaixa = statistics.getPrepareStatementCount();

        caixa(LocalDate.of(2026, 8, 2), StatusCaixa.FECHADO, "100", "100", "0");
        caixa(LocalDate.of(2026, 8, 3), StatusCaixa.FECHADO, "100", "100", "0");
        caixa(LocalDate.of(2026, 8, 4), StatusCaixa.FECHADO, "100", "100", "0");
        caixa(LocalDate.of(2026, 8, 5), StatusCaixa.FECHADO, "100", "100", "0");
        statistics.clear();

        List<CaixaResponse> cincoCaixas = caixaService.listarComResposta();
        long consultasComCincoCaixas = statistics.getPrepareStatementCount();

        assertThat(umCaixa).hasSize(1);
        assertThat(cincoCaixas).hasSize(5);
        assertThat(consultasComUmCaixa).isEqualTo(3);
        assertThat(consultasComCincoCaixas).isEqualTo(consultasComUmCaixa);
    }

    private Caixa caixa(
            LocalDate data,
            StatusCaixa status,
            String valorInicial,
            String valorApurado,
            String diferenca
    ) {
        Caixa caixa = new Caixa();
        caixa.setDataCaixa(data);
        caixa.setValorInicial(new BigDecimal(valorInicial));
        caixa.setStatus(status);
        caixa.setUsuarioAbertura(usuario);
        Instant abertoEm = Instant.now();
        caixa.setAbertoEm(abertoEm);
        if (status == StatusCaixa.FECHADO) {
            caixa.setValorApurado(new BigDecimal(valorApurado));
            caixa.setDiferenca(new BigDecimal(diferenca));
            caixa.setUsuarioFechamento(usuario);
            caixa.setFechadoEm(abertoEm.plusSeconds(1));
        }
        return caixaRepository.saveAndFlush(caixa);
    }

    private Venda venda(Caixa caixa, StatusVenda status, String valor) {
        Venda venda = new Venda();
        venda.setCaixa(caixa);
        venda.setDataVenda(caixa.getDataCaixa());
        venda.setValorTotal(new BigDecimal(valor));
        venda.setStatus(status);
        return vendaRepository.saveAndFlush(venda);
    }

    private void pagamento(Venda venda, FormaPagamento formaPagamento, String valor) {
        VendaPagamento pagamento = new VendaPagamento();
        pagamento.setVenda(venda);
        pagamento.setFormaPagamento(formaPagamento);
        pagamento.setValor(new BigDecimal(valor));
        pagamentoRepository.saveAndFlush(pagamento);
    }

    private void movimentacao(
            Caixa caixa,
            TipoMovimentacao tipo,
            FormaPagamento formaPagamento,
            String valor
    ) {
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setCaixa(caixa);
        movimentacao.setTipo(tipo);
        movimentacao.setOrigem(OrigemMovimentacao.MANUAL);
        movimentacao.setDescricao("Movimentação do histórico");
        movimentacao.setValor(new BigDecimal(valor));
        movimentacao.setDataMovimentacao(caixa.getDataCaixa());
        movimentacao.setCategoria(categoriaDespesa);
        movimentacao.setFormaPagamento(formaPagamento);
        movimentacaoRepository.saveAndFlush(movimentacao);
    }
}
