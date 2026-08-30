package br.com.barbeirofinanceiro.domain.venda;

import br.com.barbeirofinanceiro.domain.item.TipoItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemRelatorioProjection(
        UUID itemId,
        String nome,
        TipoItem tipo,
        Long quantidade,
        BigDecimal faturamento
) {
}