package br.com.barbeirofinanceiro.application.dashboard;

import java.util.List;

public record DashboardResponse(
        PeriodoDashboardResponse periodo,
        ResumoDashboardResponse resumo,
        PagamentosDashboardResponse pagamentos,
        CaixaDashboardResponse caixaAtual,
        List<ItemMaisVendidoResponse> itensMaisVendidos,
        List<FaturamentoDiarioResponse> evolucaoFaturamento
) {
}
