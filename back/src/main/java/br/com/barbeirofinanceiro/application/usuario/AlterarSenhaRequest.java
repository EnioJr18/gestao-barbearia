package br.com.barbeirofinanceiro.application.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
        @NotBlank(message = "senhaAtual é obrigatória")
        String senhaAtual,

        @NotBlank(message = "novaSenha é obrigatória")
        @Size(min = 6, max = 255, message = "novaSenha deve possuir entre 6 e 255 caracteres")
        String novaSenha
) {
}
