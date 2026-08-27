package br.com.barbeirofinanceiro.application.cliente;

import jakarta.validation.constraints.NotBlank;

public record CriarClienteRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        String telefone
) {
}
