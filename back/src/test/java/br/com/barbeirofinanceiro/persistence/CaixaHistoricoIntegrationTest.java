package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.application.auth.JwtService;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

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
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;
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
    private String token;

    @BeforeEach
    void preparar() {
        usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Histórico");
        usuario.setEmail(USER_EMAIL);
        usuario.setSenhaHash(passwordEncoder.encode("senha-teste"));
        usuarioRepository.saveAndFlush(usuario);

        token = jwtService.gerarToken(User.withUsername(USER_EMAIL)
                .password(usuario.getSenhaHash())
                .roles("USER")
                .build());

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
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void deveManterContratoParaCaixaSemMovimentacoes() throws Exception {
        Caixa caixa = caixa(LocalDate.of(2026, 8, 1), StatusCaixa.ABERTO, "100", null, null);

        mockMvc.perform(get("/api/v1/caixas/historico").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(caixa.getId().toString()))
                .andExpect(jsonPath("$.content[0].dataCaixa").value("2026-08-01"))
                .andExpect(jsonPath("$.content[0].valorInicial").value(100))
                .andExpect(jsonPath("$.content[0].entradasDinheiro").value(0))
                .andExpect(jsonPath("$.content[0].saidasDinheiro").value(0))
                .andExpect(jsonPath("$.content[0].dinheiro").value(0))
                .andExpect(jsonPath("$.content[0].pix").value(0))
                .andExpect(jsonPath("$.content[0].debito").value(0))
                .andExpect(jsonPath("$.content[0].credito").value(0))
                .andExpect(jsonPath("$.content[0].valorEsperado").value(100))
                .andExpect(jsonPath("$.content[0].status").value("ABERTO"))
                .andExpect(jsonPath("$.content[0].usuarioAberturaId").value(usuario.getId().toString()))
                .andExpect(jsonPath("$.content[0].abertoEm").isNotEmpty())
                .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].updatedAt").isNotEmpty());
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
                .andExpect(jsonPath("$.content[0].entradasDinheiro").value(10))
                .andExpect(jsonPath("$.content[0].saidasDinheiro").value(15))
                .andExpect(jsonPath("$.content[0].dinheiro").value(10))
                .andExpect(jsonPath("$.content[0].pix").value(20))
                .andExpect(jsonPath("$.content[0].credito").value(30))
                .andExpect(jsonPath("$.content[0].debito").value(40))
                .andExpect(jsonPath("$.content[0].valorEsperado").value(95));
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
                .andExpect(jsonPath("$.content[0].id").value(recente.getId().toString()))
                .andExpect(jsonPath("$.content[0].dinheiro").value(0))
                .andExpect(jsonPath("$.content[0].pix").value(22))
                .andExpect(jsonPath("$.content[0].saidasDinheiro").value(2))
                .andExpect(jsonPath("$.content[0].valorEsperado").value(195))
                .andExpect(jsonPath("$.content[1].id").value(antigo.getId().toString()))
                .andExpect(jsonPath("$.content[1].dinheiro").value(11))
                .andExpect(jsonPath("$.content[1].pix").value(0))
                .andExpect(jsonPath("$.content[1].saidasDinheiro").value(1))
                .andExpect(jsonPath("$.content[1].valorEsperado").value(110));
    }

    @Test
    void quantidadeDeConsultasPermaneceConstanteComMaisCaixas() {
        Caixa caixaInicial = caixa(LocalDate.of(2026, 8, 1), StatusCaixa.FECHADO, "100", "109", "0");
        registrarFinanceiro(caixaInicial, "10");
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<CaixaResponse> umCaixa = caixaService.listarComResposta(
                null, null, null, 0, 100).content();
        long consultasComUmCaixa = statistics.getPrepareStatementCount();

        caixa(LocalDate.of(2026, 8, 2), StatusCaixa.FECHADO, "100", "109", "0");
        caixa(LocalDate.of(2026, 8, 3), StatusCaixa.FECHADO, "100", "109", "0");
        caixa(LocalDate.of(2026, 8, 4), StatusCaixa.FECHADO, "100", "109", "0");
        caixa(LocalDate.of(2026, 8, 5), StatusCaixa.FECHADO, "100", "109", "0");
        List<Caixa> caixas = caixaRepository.findAll();
        caixas.stream().filter(c -> !c.getId().equals(caixaInicial.getId()))
                .forEach(c -> registrarFinanceiro(c, "10"));
        statistics.clear();

        List<CaixaResponse> cincoCaixas = caixaService.listarComResposta(
                null, null, null, 0, 100).content();
        long consultasComCincoCaixas = statistics.getPrepareStatementCount();

        assertThat(umCaixa).hasSize(1);
        assertThat(cincoCaixas).hasSize(5);
        assertThat(consultasComCincoCaixas).isEqualTo(consultasComUmCaixa);
    }

    @Test
    void devePaginarHistoricoComOrdenacaoEValoresDoContrato() throws Exception {
        caixa(LocalDate.of(2026, 8, 1), StatusCaixa.FECHADO, "100", "100", "0");
        Caixa meio = caixa(LocalDate.of(2026, 8, 2), StatusCaixa.ABERTO, "100", null, null);
        caixa(LocalDate.of(2026, 8, 3), StatusCaixa.FECHADO, "100", "100", "0");

        mockMvc.perform(get("/api/v1/caixas/historico").param("page", "0").param("size", "2")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].dataCaixa").value("2026-08-03"))
                .andExpect(jsonPath("$.content[1].id").value(meio.getId().toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/v1/caixas/historico").param("page", "1").param("size", "2")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void deveAplicarCadaFiltroDeStatusIsoladamente() throws Exception {
        caixa(LocalDate.of(2026, 8, 1), StatusCaixa.ABERTO, "100", null, null);
        caixa(LocalDate.of(2026, 8, 2), StatusCaixa.FECHADO, "100", "100", "0");

        mockMvc.perform(get("/api/v1/caixas/historico").param("status", "ABERTO")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ABERTO"));

        mockMvc.perform(get("/api/v1/caixas/historico").param("status", "FECHADO")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("FECHADO"));
    }

    @Test
    void deveAplicarPeriodoIsoladoComLimitesInclusivos() throws Exception {
        Caixa anterior = caixa(LocalDate.of(2026, 8, 9), StatusCaixa.FECHADO, "100", "100", "0");
        Caixa inicio = caixa(LocalDate.of(2026, 8, 10), StatusCaixa.FECHADO, "100", "100", "0");
        Caixa fim = caixa(LocalDate.of(2026, 8, 12), StatusCaixa.FECHADO, "100", "100", "0");
        Caixa posterior = caixa(LocalDate.of(2026, 8, 13), StatusCaixa.FECHADO, "100", "100", "0");

        MvcResult resultado = mockMvc.perform(get("/api/v1/caixas/historico")
                        .param("dataInicial", "2026-08-10")
                        .param("dataFinal", "2026-08-12")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn();

        List<String> ids = idsDaPagina(resultado);
        assertThat(ids).containsExactly(fim.getId().toString(), inicio.getId().toString());
        assertThat(ids).doesNotContain(anterior.getId().toString(), posterior.getId().toString());
    }

    @Test
    void deveRetornarPaginasComIdsEstaveisSemRepeticao() throws Exception {
        Instant mesmoInstante = Instant.parse("2026-08-01T12:00:00Z");
        List<Caixa> caixas = IntStream.range(0, 25)
                .mapToObj(i -> {
                    Caixa caixa = caixa(LocalDate.of(2026, 8, 1), StatusCaixa.FECHADO,
                            "100", "100", "0");
                    caixa.setAbertoEm(mesmoInstante);
                    return caixaRepository.saveAndFlush(caixa);
                })
                .toList();

        List<String> idsEsperados = caixas.stream()
                .map(caixa -> caixa.getId().toString())
                .sorted(Comparator.reverseOrder())
                .toList();
        List<String> idsRetornados = new ArrayList<>();

        for (int pagina = 0; pagina < 3; pagina++) {
            MvcResult resultado = mockMvc.perform(get("/api/v1/caixas/historico")
                            .param("page", String.valueOf(pagina))
                            .param("size", "10")
                            .with(user(USER_EMAIL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(pagina))
                    .andReturn();
            List<String> idsPagina = idsDaPagina(resultado);
            int inicioEsperado = pagina * 10;
            int fimEsperado = Math.min(inicioEsperado + 10, idsEsperados.size());
            assertThat(idsPagina).containsExactlyElementsOf(idsEsperados.subList(inicioEsperado, fimEsperado));
            idsRetornados.addAll(idsPagina);
        }

        assertThat(idsRetornados).containsExactlyElementsOf(idsEsperados);
        assertThat(idsRetornados).doesNotHaveDuplicates();
    }

    @Test
    void deveRetornarUltimaPaginaValidaEAplicarFiltrosInclusivos() throws Exception {
        caixa(LocalDate.of(2026, 8, 1), StatusCaixa.FECHADO, "100", "100", "0");
        Caixa caixaFiltrado = caixa(LocalDate.of(2026, 8, 2), StatusCaixa.ABERTO, "100", null, null);
        caixa(LocalDate.of(2026, 8, 3), StatusCaixa.FECHADO, "100", "100", "0");

        mockMvc.perform(get("/api/v1/caixas/historico")
                        .param("status", "ABERTO")
                        .param("dataInicial", "2026-08-02")
                        .param("dataFinal", "2026-08-02")
                        .param("page", "9")
                        .param("size", "1")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(caixaFiltrado.getId().toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void deveValidarDatasEPaginacaoDoHistorico() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/historico").param("dataInicial", "2026-08-01")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dataInicial e dataFinal devem ser informadas juntas"));

        mockMvc.perform(get("/api/v1/caixas/historico").param("dataFinal", "2026-08-01")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dataInicial e dataFinal devem ser informadas juntas"));

        mockMvc.perform(get("/api/v1/caixas/historico").param("dataInicial", "2026-08-02")
                        .param("dataFinal", "2026-08-01").with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dataInicial deve ser anterior ou igual a dataFinal"));

        mockMvc.perform(get("/api/v1/caixas/historico").param("dataInicial", "data-invalida")
                        .param("dataFinal", "2026-08-01").with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro 'dataInicial' inválido"));

        mockMvc.perform(get("/api/v1/caixas/historico").param("status", "DESCONHECIDO")
                        .with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro 'status' inválido"));

        mockMvc.perform(get("/api/v1/caixas/historico").param("page", "-1").with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/caixas/historico").param("size", "-1").with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/caixas/historico").param("size", "0").with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/caixas/historico").param("size", "101").with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/caixas/historico").param("page", "abc").with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro 'page' inválido"));
        mockMvc.perform(get("/api/v1/caixas/historico").param("size", "abc").with(user(USER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro 'size' inválido"));

        mockMvc.perform(get("/api/v1/caixas/historico").param("size", "100").with(user(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void deveExigirAutenticacaoParaConsultarHistorico() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/historico"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveAutorizarConsultaDoHistoricoComJwtValido() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/historico")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0));
    }

    private void registrarFinanceiro(Caixa caixa, String valor) {
        Venda venda = venda(caixa, StatusVenda.FINALIZADA, valor);
        pagamento(venda, FormaPagamento.DINHEIRO, valor);
        movimentacao(caixa, TipoMovimentacao.DESPESA, FormaPagamento.DINHEIRO, "1");
    }

    private List<String> idsDaPagina(MvcResult resultado) throws Exception {
        JsonNode content = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .path("content");
        List<String> ids = new ArrayList<>();
        content.forEach(item -> ids.add(item.path("id").asText()));
        return ids;
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
