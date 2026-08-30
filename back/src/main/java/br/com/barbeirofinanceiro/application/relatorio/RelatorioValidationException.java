package br.com.barbeirofinanceiro.application.relatorio;

public class RelatorioValidationException extends RuntimeException {
    public RelatorioValidationException(String message) {
        super(message);
    }
}