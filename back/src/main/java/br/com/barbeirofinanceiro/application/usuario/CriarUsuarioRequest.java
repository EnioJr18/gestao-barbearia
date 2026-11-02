package br.com.barbeirofinanceiro.application.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120, message = "nome deve possuir no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email deve ser válido")
        @Size(max = 255, message = "email deve possuir no máximo 255 caracteres")
        String email,

        @NotBlank(message = "senha é obrigatória")
        @Size(min = 6, max = 255, message = "senha deve possuir entre 6 e 255 caracteres")
        String senha
) {
}
