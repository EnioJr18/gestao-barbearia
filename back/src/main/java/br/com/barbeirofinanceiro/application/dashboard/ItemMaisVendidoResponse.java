package br.com.barbeirofinanceiro.application.dashboard;

import br.com.barbeirofinanceiro.domain.item.TipoItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemMaisVendidoResponse(
        UUID itemId,
        String nome,
        TipoItem tipo,
        long quantidade,
        BigDecimal faturamento
) {
}
