package br.com.barbeirofinanceiro.application.caixa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FecharCaixaRequest(
        @NotNull(message = "valorApurado é obrigatório")
        @DecimalMin(value = "0.00", inclusive = true, message = "valorApurado deve ser maior ou igual a zero")
        @Digits(integer = 10, fraction = 2, message = "valorApurado deve ter no máximo duas casas decimais")
        BigDecimal valorApurado
) {
}
