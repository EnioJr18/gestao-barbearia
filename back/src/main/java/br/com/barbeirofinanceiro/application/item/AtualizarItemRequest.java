package br.com.barbeirofinanceiro.application.item;

import br.com.barbeirofinanceiro.domain.item.TipoItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AtualizarItemRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotNull(message = "tipo é obrigatório") TipoItem tipo,
        @NotNull(message = "preco é obrigatório")
        @DecimalMin(value = "0.00", message = "preco deve ser maior ou igual a zero") BigDecimal preco
) {
}
