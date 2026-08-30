package br.com.barbeirofinanceiro.persistence;

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
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RelatorioControllerIntegrationTest extends PostgresPersistenceTest {

    private static final String USER = "relatorio-user@teste.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CaixaRepository caixaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    private Caixa caixa;
    private Categoria categoriaDespesa;
    private Categoria categoriaReceita;

    @BeforeEach
    void preparar() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Relatório");
        usuario.setEmail(USER);
        usuario.setSenhaHash("hash");
        usuarioRepository.saveAndFlush(usuario);

        caixa = new Caixa();
        caixa.setDataCaixa(LocalDate.of(2026, 8, 23));
        caixa.setValorInicial(BigDecimal.ZERO);
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setUsuarioAbertura(usuario);
        caixaRepository.saveAndFlush(caixa);

        String sufixo = String.valueOf(System.nanoTime());

        categoriaDespesa = newEntity(Categoria.class);
        categoriaDespesa.setNome("Operacional " + sufixo);
        categoriaDespesa.setTipo(TipoCategoria.DESPESA);
        categoriaRepository.saveAndFlush(categoriaDespesa);

        categoriaReceita = newEntity(Categoria.class);
        categoriaReceita.setNome("Receita " + sufixo);
        categoriaReceita.setTipo(TipoCategoria.RECEITA);
        categoriaRepository.saveAndFlush(categoriaReceita);
    }

    @AfterEach
    void limpar() {
        movimentacaoRepository.deleteAll();
        vendaRepository.deleteAll();
        caixaRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveRetornarResumoDoPeriodoComResultadoIgualAFaturamentoMenosDespesas() throws Exception {
        venda(BigDecimal.valueOf(500), LocalDate.of(2026, 8, 24), StatusVenda.FINALIZADA);
        venda(BigDecimal.valueOf(300), LocalDate.of(2026, 8, 24), StatusVenda.CANCELADA);

        movimentacao(TipoMovimentacao.DESPESA, BigDecimal.valueOf(100), LocalDate.of(2026, 8, 23), categoriaDespesa);
        movimentacao(TipoMovimentacao.DESPESA, BigDecimal.valueOf(50), LocalDate.of(2026, 8, 24), categoriaDespesa);
        movimentacao(TipoMovimentacao.DESPESA, BigDecimal.valueOf(200), LocalDate.of(2026, 8, 25), categoriaDespesa);
        movimentacao(TipoMovimentacao.RECEITA, BigDecimal.valueOf(500), LocalDate.of(2026, 8, 24), categoriaReceita);

        mockMvc.perform(get("/api/v1/relatorios/resumo")
                        .param("dataInicial", "2026-08-23")
                        .param("dataFinal", "2026-08-24")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamento").value(500))
                .andExpect(jsonPath("$.despesas").value(150))
                .andExpect(jsonPath("$.resultado").value(350))
                .andExpect(jsonPath("$.quantidadeVendas").value(1));
    }

    @Test
    void deveRetornarBadRequestQuandoDataInicialForPosteriorADataFinal() throws Exception {
        mockMvc.perform(get("/api/v1/relatorios/resumo")
                        .param("dataInicial", "2026-08-25")
                        .param("dataFinal", "2026-08-24")
                        .with(user(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("A data inicial não pode ser posterior à data final"));
    }

    private void venda(BigDecimal valor, LocalDate data, StatusVenda status) {
        Venda venda = newEntity(Venda.class);
        venda.setCaixa(caixa);
        venda.setDataVenda(data);
        venda.setValorTotal(valor);
        venda.setStatus(status);
        vendaRepository.saveAndFlush(venda);
    }

    private void movimentacao(
            TipoMovimentacao tipo,
            BigDecimal valor,
            LocalDate data,
            Categoria categoria
    ) {
        Movimentacao movimentacao = newEntity(Movimentacao.class);
        movimentacao.setTipo(tipo);
        movimentacao.setOrigem(OrigemMovimentacao.MANUAL);
        movimentacao.setDescricao("Movimentacao para relatório");
        movimentacao.setValor(valor);
        movimentacao.setDataMovimentacao(data);
        movimentacao.setCategoria(categoria);
        movimentacao.setFormaPagamento(FormaPagamento.DINHEIRO);
        movimentacaoRepository.saveAndFlush(movimentacao);
    }
}