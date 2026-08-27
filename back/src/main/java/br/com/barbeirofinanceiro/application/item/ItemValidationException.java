package br.com.barbeirofinanceiro.application.item;

public class ItemValidationException extends RuntimeException {
    public ItemValidationException(String message) {
        super(message);
    }
}
