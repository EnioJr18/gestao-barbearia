package br.com.barbeirofinanceiro.application.caixa;

public class CaixaNotFoundException extends RuntimeException {
    public CaixaNotFoundException(String message) { super(message); }
}
