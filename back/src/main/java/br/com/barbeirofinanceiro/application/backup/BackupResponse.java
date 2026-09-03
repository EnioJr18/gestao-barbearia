package br.com.barbeirofinanceiro.application.backup;

import br.com.barbeirofinanceiro.domain.backup.BackupStatus;

import java.time.Instant;

public record BackupResponse(
        String arquivo,
        Instant dataHora,
        long tamanhoBytes,
        BackupStatus status
) {
}
