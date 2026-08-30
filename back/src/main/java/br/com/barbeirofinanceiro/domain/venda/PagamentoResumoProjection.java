package br.com.barbeirofinanceiro.domain.venda;

import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;

import java.math.BigDecimal;

public record PagamentoResumoProjection(
        FormaPagamento formaPagamento,
        BigDecimal valor
) {
}