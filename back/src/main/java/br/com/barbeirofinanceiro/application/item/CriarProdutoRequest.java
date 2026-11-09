package br.com.barbeirofinanceiro.application.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CriarProdutoRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotNull(message = "preco é obrigatório")
        @DecimalMin(value = "0.00", message = "preco deve ser maior ou igual a zero") BigDecimal preco,
        @NotNull(message = "estoque é obrigatório") Integer estoque
) {
}
