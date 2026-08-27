package br.com.barbeirofinanceiro.application.item;

import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.TipoItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        String nome,
        TipoItem tipo,
        BigDecimal preco,
        Integer estoque,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getId(), item.getNome(), item.getTipo(), item.getPreco(), item.getEstoque(),
                item.isAtivo(), item.getCreatedAt(), item.getUpdatedAt()
        );
    }
}
