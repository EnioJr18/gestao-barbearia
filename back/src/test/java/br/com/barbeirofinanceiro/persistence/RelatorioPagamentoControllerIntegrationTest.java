package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import br.com.barbeirofinanceiro.domain.venda.StatusVenda;
import br.com.barbeirofinanceiro.domain.venda.Venda;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamento;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamentoRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
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
class RelatorioPagamentoControllerIntegrationTest extends PostgresPersistenceTest {

    private static final String USER = "relatorio-pagamentos-user@teste.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CaixaRepository caixaRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private VendaPagamentoRepository vendaPagamentoRepository;

    private Caixa caixa;

    @BeforeEach
    void preparar() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Relatório Pagamentos");
        usuario.setEmail(USER);
        usuario.setSenhaHash("hash");
        usuarioRepository.saveAndFlush(usuario);

        caixa = new Caixa();
        caixa.setDataCaixa(LocalDate.of(2026, 8, 23));
        caixa.setValorInicial(BigDecimal.ZERO);
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setUsuarioAbertura(usuario);
        caixaRepository.saveAndFlush(caixa);
    }

    @AfterEach
    void limpar() {
        vendaPagamentoRepository.deleteAll();
        vendaRepository.deleteAll();
        caixaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveRetornarResumoDePagamentosPorForma() throws Exception {
        Venda vendaFinalizada1 = venda(BigDecimal.valueOf(100), LocalDate.of(2026, 8, 23), StatusVenda.FINALIZADA);
        pagamento(vendaFinalizada1, FormaPagamento.DINHEIRO, BigDecimal.valueOf(60));
        pagamento(vendaFinalizada1, FormaPagamento.PIX, BigDecimal.valueOf(40));

        Venda vendaFinalizada2 = venda(BigDecimal.valueOf(50), LocalDate.of(2026, 8, 24), StatusVenda.FINALIZADA);
        pagamento(vendaFinalizada2, FormaPagamento.CARTAO_CREDITO, BigDecimal.valueOf(50));

        Venda vendaCancelada = venda(BigDecimal.valueOf(200), LocalDate.of(2026, 8, 24), StatusVenda.CANCELADA);
        pagamento(vendaCancelada, FormaPagamento.DINHEIRO, BigDecimal.valueOf(200));

        mockMvc.perform(get("/api/v1/relatorios/pagamentos")
                        .param("dataInicial", "2026-08-23")
                        .param("dataFinal", "2026-08-24")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dinheiro").value(60))
                .andExpect(jsonPath("$.pix").value(40))
                .andExpect(jsonPath("$.cartaoCredito").value(50))
                .andExpect(jsonPath("$.cartaoDebito").value(0));
    }

    @Test
    void deveRetornarBadRequestQuandoPeriodoDePagamentosForInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/relatorios/pagamentos")
                        .param("dataInicial", "2026-08-25")
                        .param("dataFinal", "2026-08-24")
                        .with(user(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("A data inicial não pode ser posterior à data final"));
    }

    private Venda venda(BigDecimal valor, LocalDate data, StatusVenda status) {
        Venda venda = newEntity(Venda.class);
        venda.setCaixa(caixa);
        venda.setDataVenda(data);
        venda.setValorTotal(valor);
        venda.setStatus(status);
        return vendaRepository.saveAndFlush(venda);
    }

    private void pagamento(Venda venda, FormaPagamento formaPagamento, BigDecimal valor) {
        VendaPagamento pagamento = newEntity(VendaPagamento.class);
        pagamento.setVenda(venda);
        pagamento.setFormaPagamento(formaPagamento);
        pagamento.setValor(valor);
        vendaPagamentoRepository.saveAndFlush(pagamento);
    }
}