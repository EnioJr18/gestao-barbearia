package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.application.relatorio.ItemRelatorioResponse;
import br.com.barbeirofinanceiro.application.relatorio.RelatorioResumoResponse;
import br.com.barbeirofinanceiro.application.relatorio.RelatorioService;
import br.com.barbeirofinanceiro.application.relatorio.RelatorioValidationException;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
import br.com.barbeirofinanceiro.domain.movimentacao.DespesaResumoProjection;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import br.com.barbeirofinanceiro.domain.movimentacao.MovimentacaoRepository;
import br.com.barbeirofinanceiro.domain.venda.ItemRelatorioProjection;
import br.com.barbeirofinanceiro.domain.venda.ItemVendaRepository;
import br.com.barbeirofinanceiro.domain.venda.PagamentoResumoProjection;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamentoRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaResumoProjection;
import br.com.barbeirofinanceiro.application.relatorio.RelatorioPagamentoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RelatorioServiceTest {

    private final VendaRepository vendaRepository =
            mock(VendaRepository.class);

    private final VendaPagamentoRepository vendaPagamentoRepository =
            mock(VendaPagamentoRepository.class);

    private final MovimentacaoRepository movimentacaoRepository =
            mock(MovimentacaoRepository.class);

    private final ItemVendaRepository itemVendaRepository =
            mock(ItemVendaRepository.class);

    private final RelatorioService service = new RelatorioService(
            vendaRepository,
            vendaPagamentoRepository,
            movimentacaoRepository,
            itemVendaRepository
    );

    @Test
    void deveCalcularResumoComResultadoIgualAFaturamentoMenosDespesas() {
        LocalDate dataInicial = LocalDate.of(2026, 8, 23);
        LocalDate dataFinal = LocalDate.of(2026, 8, 24);

        when(vendaRepository.findResumoByDataVendaBetween(dataInicial, dataFinal))
                .thenReturn(
                        new VendaResumoProjection(
                                BigDecimal.valueOf(500),
                                1
                        )
                );

        when(movimentacaoRepository.buscarResumoDespesas(dataInicial, dataFinal))
                .thenReturn(
                        new DespesaResumoProjection(
                                BigDecimal.valueOf(150)
                        )
                );

        RelatorioResumoResponse resumo =
                service.buscarResumo(dataInicial, dataFinal);

        assertThat(resumo.faturamento())
                .isEqualByComparingTo(BigDecimal.valueOf(500));

        assertThat(resumo.despesas())
                .isEqualByComparingTo(BigDecimal.valueOf(150));

        assertThat(resumo.resultado())
                .isEqualByComparingTo(BigDecimal.valueOf(350));

        assertThat(resumo.quantidadeVendas())
                .isEqualTo(1);
    }

    @Test
    void deveDelegarConsultasParaRepositoriosComMesmoPeriodo() {
        LocalDate dataInicial = LocalDate.of(2026, 8, 23);
        LocalDate dataFinal = LocalDate.of(2026, 8, 24);

        when(vendaRepository.findResumoByDataVendaBetween(dataInicial, dataFinal))
                .thenReturn(
                        new VendaResumoProjection(
                                BigDecimal.ZERO,
                                0
                        )
                );

        when(movimentacaoRepository.buscarResumoDespesas(dataInicial, dataFinal))
                .thenReturn(
                        new DespesaResumoProjection(
                                BigDecimal.ZERO
                        )
                );

        service.buscarResumo(dataInicial, dataFinal);

        verify(vendaRepository)
                .findResumoByDataVendaBetween(dataInicial, dataFinal);

        verify(movimentacaoRepository)
                .buscarResumoDespesas(dataInicial, dataFinal);
    }

    @Test
    void deveLancarExcecaoQuandoDataInicialForPosteriorADataFinal() {
        LocalDate dataInicial = LocalDate.of(2026, 8, 25);
        LocalDate dataFinal = LocalDate.of(2026, 8, 24);

        assertThatThrownBy(() ->
                service.buscarResumo(dataInicial, dataFinal)
        )
                .isInstanceOf(RelatorioValidationException.class)
                .hasMessage(
                        "A data inicial não pode ser posterior à data final"
                );

        verifyNoInteractions(
                vendaRepository,
                movimentacaoRepository
        );
    }

    @Test
    void deveMapearResumoDePagamentosPorFormaComZeroParaFormasAusentes() {
        LocalDate dataInicial = LocalDate.of(2026, 8, 23);
        LocalDate dataFinal = LocalDate.of(2026, 8, 24);

        when(vendaPagamentoRepository.buscarResumoPorFormaPagamento(
                dataInicial,
                dataFinal
        )).thenReturn(
                List.of(
                        new PagamentoResumoProjection(
                                FormaPagamento.DINHEIRO,
                                BigDecimal.valueOf(60)
                        ),
                        new PagamentoResumoProjection(
                                FormaPagamento.PIX,
                                BigDecimal.valueOf(40)
                        ),
                        new PagamentoResumoProjection(
                                FormaPagamento.CARTAO_CREDITO,
                                BigDecimal.valueOf(50)
                        )
                )
        );

        RelatorioPagamentoResponse resposta =
                service.buscarPagamentos(dataInicial, dataFinal);

        assertThat(resposta.dinheiro())
                .isEqualByComparingTo(BigDecimal.valueOf(60));

        assertThat(resposta.pix())
                .isEqualByComparingTo(BigDecimal.valueOf(40));

        assertThat(resposta.cartaoCredito())
                .isEqualByComparingTo(BigDecimal.valueOf(50));

        assertThat(resposta.cartaoDebito())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deveLancarExcecaoQuandoPeriodoDePagamentosForInvalido() {
        LocalDate dataInicial = LocalDate.of(2026, 8, 25);
        LocalDate dataFinal = LocalDate.of(2026, 8, 24);

        assertThatThrownBy(() ->
                service.buscarPagamentos(dataInicial, dataFinal)
        )
                .isInstanceOf(RelatorioValidationException.class)
                .hasMessage(
                        "A data inicial não pode ser posterior à data final"
                );

        verifyNoInteractions(vendaPagamentoRepository);
    }

    @Test
    void deveMapearResumoDeItens() {
        LocalDate dataInicial = LocalDate.of(2026, 8, 23);
        LocalDate dataFinal = LocalDate.of(2026, 8, 24);

        UUID itemId = UUID.randomUUID();

        when(itemVendaRepository.buscarResumoItens(
                dataInicial,
                dataFinal
        )).thenReturn(
                List.of(
                        new ItemRelatorioProjection(
                                itemId,
                                "Corte",
                                TipoItem.SERVICO,
                                3L,
                                BigDecimal.valueOf(90)
                        ),
                        new ItemRelatorioProjection(
                                UUID.randomUUID(),
                                "Pomada",
                                TipoItem.PRODUTO,
                                1L,
                                BigDecimal.valueOf(35)
                        )
                )
        );

        List<ItemRelatorioResponse> resposta =
                service.buscarItens(dataInicial, dataFinal);

        assertThat(resposta)
                .hasSize(2);

        assertThat(resposta.get(0).itemId())
                .isEqualTo(itemId);

        assertThat(resposta.get(0).nome())
                .isEqualTo("Corte");

        assertThat(resposta.get(0).tipo())
                .isEqualTo(TipoItem.SERVICO);

        assertThat(resposta.get(0).quantidade())
                .isEqualTo(3L);

        assertThat(resposta.get(0).faturamento())
                .isEqualByComparingTo(BigDecimal.valueOf(90));

        assertThat(resposta.get(1).nome())
                .isEqualTo("Pomada");
    }
}