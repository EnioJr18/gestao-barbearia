package br.com.barbeirofinanceiro.application.backup;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Component
public class ManutencaoCoordinator {

    private final ReentrantReadWriteLock operacoesCriticas = new ReentrantReadWriteLock(true);
    private final ReentrantLock operacaoBackup = new ReentrantLock(true);

    public void executarEscrita(Supplier<Void> operacao) {
        executarEscritaComResultado(operacao);
    }

    public <T> T executarEscritaComResultado(Supplier<T> operacao) {
        try {
            if (!operacoesCriticas.readLock().tryLock(0, TimeUnit.SECONDS)) {
                throw new BackupConflictException("Sistema em manutenção para restauração de backup");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BackupConflictException("Sistema em manutenção para restauração de backup");
        }

        try {
            return operacao.get();
        } finally {
            operacoesCriticas.readLock().unlock();
        }
    }

    public <T> T executarBackup(Supplier<T> operacao) {
        if (!operacaoBackup.tryLock()) {
            throw new BackupConflictException("Já existe uma operação de backup ou restauração em andamento");
        }
        try {
            return operacao.get();
        } finally {
            operacaoBackup.unlock();
        }
    }

    public <T> T executarRestauracao(Supplier<T> operacao) {
        return executarBackup(() -> {
            operacoesCriticas.writeLock().lock();
            try {
                return operacao.get();
            } finally {
                operacoesCriticas.writeLock().unlock();
            }
        });
    }
}
