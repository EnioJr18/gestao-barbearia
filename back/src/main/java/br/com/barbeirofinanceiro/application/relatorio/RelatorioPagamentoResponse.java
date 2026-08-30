package br.com.barbeirofinanceiro.application.relatorio;

import java.math.BigDecimal;

public record RelatorioPagamentoResponse(
        BigDecimal dinheiro,
        BigDecimal pix,
        BigDecimal cartaoCredito,
        BigDecimal cartaoDebito
) {
}