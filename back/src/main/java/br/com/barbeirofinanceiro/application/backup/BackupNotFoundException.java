package br.com.barbeirofinanceiro.application.backup;

public class BackupNotFoundException extends RuntimeException {
    public BackupNotFoundException(String message) { super(message); }
}
