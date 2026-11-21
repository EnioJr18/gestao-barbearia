package br.com.barbeirofinanceiro.application.usuario;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AtualizarUsuarioRequest {

    @NotBlank(message = "nome é obrigatório")
    @Size(max = 120, message = "nome deve possuir no máximo 120 caracteres")
    private String nome;

    @NotBlank(message = "email é obrigatório")
    @Email(message = "email deve ser válido")
    @Size(max = 255, message = "email deve possuir no máximo 255 caracteres")
    private String email;

    @JsonProperty("nome")
    public void setNome(String nome) {
        this.nome = nome;
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonAnySetter
    public void rejeitarCampoNaoPermitido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo não permitido na atualização pessoal");
    }

    public String nome() {
        return nome;
    }

    public String email() {
        return email;
    }
}
