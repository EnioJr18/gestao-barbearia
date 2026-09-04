package br.com.barbeirofinanceiro.application.dashboard;

import br.com.barbeirofinanceiro.application.caixa.CaixaService;
import br.com.barbeirofinanceiro.application.relatorio.RelatorioPagamentoResponse;
import br.com.barbeirofinanceiro.application.relatorio.RelatorioResumoResponse;
import br.com.barbeirofinanceiro.application.relatorio.RelatorioService;
import br.com.barbeirofinanceiro.domain.venda.ItemRelatorioProjection;
import br.com.barbeirofinanceiro.domain.venda.ItemVendaRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class DashboardService {

    private final RelatorioService relatorioService;
    private final CaixaService caixaService;
    private final ItemVendaRepository itemVendaRepository;
    private final VendaRepository vendaRepository;

    public DashboardService(
            RelatorioService relatorioService,
            CaixaService caixaService,
            ItemVendaRepository itemVendaRepository,
            VendaRepository vendaRepository
    ) {
        this.relatorioService = relatorioService;
        this.caixaService = caixaService;
        this.itemVendaRepository = itemVendaRepository;
        this.vendaRepository = vendaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse buscar(LocalDate dataInicial, LocalDate dataFinal) {
        Periodo periodo = resolverPeriodo(dataInicial, dataFinal);
        RelatorioResumoResponse resumo = relatorioService.buscarResumo(periodo.dataInicial(), periodo.dataFinal());
        RelatorioPagamentoResponse pagamentos = relatorioService.buscarPagamentos(periodo.dataInicial(), periodo.dataFinal());

        return new DashboardResponse(
                new PeriodoDashboardResponse(periodo.dataInicial(), periodo.dataFinal()),
                new ResumoDashboardResponse(
                        resumo.faturamento(), resumo.despesas(), resumo.resultado(), resumo.quantidadeVendas()
                ),
                new PagamentosDashboardResponse(
                        pagamentos.dinheiro(), pagamentos.pix(), pagamentos.cartaoCredito(), pagamentos.cartaoDebito()
                ),
                buscarCaixaAtual(),
                itemVendaRepository.buscarResumoItens(
                                periodo.dataInicial(), periodo.dataFinal(), PageRequest.of(0, 5)
                        ).stream()
                        .map(this::mapearItem)
                        .toList(),
                vendaRepository.buscarFaturamentoDiario(periodo.dataInicial(), periodo.dataFinal()).stream()
                        .map(item -> new FaturamentoDiarioResponse(item.data(), item.faturamento()))
                        .toList()
        );
    }

    private CaixaDashboardResponse buscarCaixaAtual() {
        return caixaService.atualComRespostaOpcional()
                .map(CaixaDashboardResponse::from)
                .orElse(null);
    }

    private ItemMaisVendidoResponse mapearItem(ItemRelatorioProjection item) {
        return new ItemMaisVendidoResponse(
                item.itemId(), item.nome(), item.tipo(), item.quantidade(), item.faturamento()
        );
    }

    private Periodo resolverPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial == null && dataFinal == null) {
            YearMonth mesAtual = YearMonth.now();
            return new Periodo(mesAtual.atDay(1), mesAtual.atEndOfMonth());
        }
        if (dataInicial == null || dataFinal == null) {
            throw new DashboardValidationException("dataInicial e dataFinal devem ser informadas juntas");
        }
        if (dataInicial.isAfter(dataFinal)) {
            throw new DashboardValidationException("A data inicial não pode ser posterior à data final");
        }
        return new Periodo(dataInicial, dataFinal);
    }

    private record Periodo(LocalDate dataInicial, LocalDate dataFinal) {
    }
}
