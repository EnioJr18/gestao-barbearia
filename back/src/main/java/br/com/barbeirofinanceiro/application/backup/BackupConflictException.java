package br.com.barbeirofinanceiro.application.backup;

public class BackupConflictException extends RuntimeException {
    public BackupConflictException(String message) { super(message); }
}
