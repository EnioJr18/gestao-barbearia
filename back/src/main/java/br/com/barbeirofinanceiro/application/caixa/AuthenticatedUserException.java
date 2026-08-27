package br.com.barbeirofinanceiro.application.caixa;

public class AuthenticatedUserException extends RuntimeException {
    public AuthenticatedUserException(String message) { super(message); }
}
