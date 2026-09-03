package br.com.barbeirofinanceiro.application.usuario;

public class UsuarioConflictException extends RuntimeException {

    public UsuarioConflictException(String message) {
        super(message);
    }
}
