package br.com.barbeirofinanceiro.application.usuario;

public class UsuarioNotFoundException extends RuntimeException {

    public UsuarioNotFoundException(String message) {
        super(message);
    }
}
