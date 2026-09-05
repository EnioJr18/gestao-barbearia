package br.com.barbeirofinanceiro.application.backup;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Component
public class ManutencaoCoordinator {

    private final ReentrantReadWriteLock operacoesCriticas = new ReentrantReadWriteLock(true);
    private final ReentrantLock operacaoBackup = new ReentrantLock(true);
    private final AtomicBoolean reinicioNecessario = new AtomicBoolean(false);

    public void executarEscrita(Supplier<Void> operacao) {
        executarOperacao(operacao);
    }

    public <T> T executarOperacao(Supplier<T> operacao) {
        exigirAplicacaoDisponivel();
        try {
            if (!operacoesCriticas.readLock().tryLock(0, TimeUnit.SECONDS)) {
                throw new BackupMaintenanceException("Sistema em manutenção para restauração de backup");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BackupMaintenanceException("Sistema em manutenção para restauração de backup");
        }

        try {
            return operacao.get();
        } finally {
            operacoesCriticas.readLock().unlock();
        }
    }

    public <T> T executarBackup(Supplier<T> operacao) {
        exigirAplicacaoDisponivel();
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

    public void marcarReinicioNecessario() {
        reinicioNecessario.set(true);
    }

    public boolean reinicioNecessario() {
        return reinicioNecessario.get();
    }

    public void validarAplicacaoDisponivel() {
        exigirAplicacaoDisponivel();
    }

    private void exigirAplicacaoDisponivel() {
        if (reinicioNecessario()) {
            throw new BackupMaintenanceException(
                    "Sistema em manutenção após restauração. Reinicie o ambiente para continuar."
            );
        }
    }
}
