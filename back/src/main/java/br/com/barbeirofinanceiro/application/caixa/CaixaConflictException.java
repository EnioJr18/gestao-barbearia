package br.com.barbeirofinanceiro.application.caixa;

public class CaixaConflictException extends RuntimeException {
    public CaixaConflictException(String message) { super(message); }
}
