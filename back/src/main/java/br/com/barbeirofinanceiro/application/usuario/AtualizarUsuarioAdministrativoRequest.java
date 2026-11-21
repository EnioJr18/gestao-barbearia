package br.com.barbeirofinanceiro.application.usuario;

import br.com.barbeirofinanceiro.domain.usuario.UsuarioPerfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioAdministrativoRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120, message = "nome deve possuir no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email deve ser válido")
        @Size(max = 255, message = "email deve possuir no máximo 255 caracteres")
        String email,

        @NotNull(message = "perfil é obrigatório")
        UsuarioPerfil perfil
) {
}
