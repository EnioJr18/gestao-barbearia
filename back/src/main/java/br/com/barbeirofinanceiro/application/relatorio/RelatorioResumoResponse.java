package br.com.barbeirofinanceiro.application.relatorio;

import java.math.BigDecimal;

public record RelatorioResumoResponse(
        BigDecimal faturamento,
        BigDecimal despesas,
        BigDecimal resultado,
        long quantidadeVendas
) {
}