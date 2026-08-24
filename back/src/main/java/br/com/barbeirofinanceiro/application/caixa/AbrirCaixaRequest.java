package br.com.barbeirofinanceiro.application.caixa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AbrirCaixaRequest(
        @NotNull(message = "valorInicial é obrigatório")
        @DecimalMin(value = "0.00", inclusive = false, message = "valorInicial deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "valorInicial deve ter no máximo duas casas decimais")
        BigDecimal valorInicial
) {
}
