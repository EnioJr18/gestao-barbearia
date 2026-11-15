package br.com.barbeirofinanceiro.domain.venda;

import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;

import java.math.BigDecimal;
import java.util.UUID;

public record PagamentoCaixaResumoProjection(
        UUID caixaId,
        FormaPagamento formaPagamento,
        BigDecimal valor
) {
}
