package br.com.barbeirofinanceiro.application.item;

public class ItemConflictException extends RuntimeException {
    public ItemConflictException(String message) {
        super(message);
    }
}
