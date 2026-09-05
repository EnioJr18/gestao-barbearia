package br.com.barbeirofinanceiro.application.dashboard;

import java.math.BigDecimal;

public record PagamentosDashboardResponse(
        BigDecimal dinheiro,
        BigDecimal pix,
        BigDecimal cartaoCredito,
        BigDecimal cartaoDebito
) {
}
