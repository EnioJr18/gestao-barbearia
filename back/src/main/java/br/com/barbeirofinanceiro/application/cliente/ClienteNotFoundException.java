package br.com.barbeirofinanceiro.application.cliente;

public class ClienteNotFoundException extends RuntimeException {
    public ClienteNotFoundException(String message) { super(message); }
}
