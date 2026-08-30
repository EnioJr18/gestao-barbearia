package br.com.barbeirofinanceiro.domain.venda;

import java.math.BigDecimal;

public record VendaResumoProjection(
        BigDecimal faturamento,
        long quantidadeVendas
) {
}