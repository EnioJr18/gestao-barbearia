package br.com.barbeirofinanceiro.application.dashboard;

import java.math.BigDecimal;

public record ResumoDashboardResponse(
        BigDecimal faturamento,
        BigDecimal despesas,
        BigDecimal resultado,
        long quantidadeVendas
) {
}
