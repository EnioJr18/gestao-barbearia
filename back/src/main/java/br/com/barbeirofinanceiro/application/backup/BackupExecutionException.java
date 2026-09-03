package br.com.barbeirofinanceiro.application.backup;

public class BackupExecutionException extends RuntimeException {
    public BackupExecutionException(String message) { super(message); }
    public BackupExecutionException(String message, Throwable cause) { super(message, cause); }
}
