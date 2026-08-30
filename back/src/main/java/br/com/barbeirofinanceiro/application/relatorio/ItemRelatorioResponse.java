package br.com.barbeirofinanceiro.application.relatorio;

import br.com.barbeirofinanceiro.domain.item.TipoItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemRelatorioResponse(
        UUID itemId,
        String nome,
        TipoItem tipo,
        long quantidade,
        BigDecimal faturamento
) {
}