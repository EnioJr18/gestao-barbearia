package br.com.barbeirofinanceiro.domain.movimentacao;

import java.math.BigDecimal;
import java.util.UUID;

public record DespesaCaixaResumoProjection(UUID caixaId, BigDecimal valor) {
}
