package br.com.barbeirofinanceiro.application.backup;

import br.com.barbeirofinanceiro.config.BackupProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class PostgresBackupExecutor {

    private final BackupProperties properties;
    private final DataSourceProperties dataSourceProperties;

    public PostgresBackupExecutor(BackupProperties properties, DataSourceProperties dataSourceProperties) {
        this.properties = properties;
        this.dataSourceProperties = dataSourceProperties;
    }

    public void criarBackup(Path arquivoTemporario) {
        executar(
                List.of(
                        properties.getPgDump(),
                        "-Fc",
                        "--no-owner",
                        "--no-privileges",
                        "--file=" + arquivoTemporario.toAbsolutePath()
                ),
                "gerar o backup"
        );
    }

    public void validarBackup(Path arquivo) {
        executar(List.of(properties.getPgRestore(), "--list", arquivo.toAbsolutePath().toString()), "validar o backup");
    }

    public void restaurarBackup(Path arquivo) {
        executar(
                List.of(
                        properties.getPgRestore(),
                        "--clean",
                        "--if-exists",
                        "--no-owner",
                        "--no-privileges",
                        "--single-transaction",
                        "--dbname=" + banco()
                        , arquivo.toAbsolutePath().toString()
                ),
                "restaurar o backup"
        );
    }

    private void executar(List<String> comando, String operacao) {
        ProcessBuilder processBuilder = new ProcessBuilder(comando);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.environment().putAll(ambientePostgres());

        try {
            Process processo = processBuilder.start();
            if (!processo.waitFor(properties.getTimeoutSegundos(), TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                throw new BackupExecutionException("Tempo limite excedido ao " + operacao);
            }
            if (processo.exitValue() != 0) {
                throw new BackupExecutionException("Falha ao " + operacao);
            }
        } catch (IOException exception) {
            throw new BackupExecutionException(
                    "Utilitário PostgreSQL não encontrado ou indisponível. Configure backup.pg-dump e backup.pg-restore para execução local.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BackupExecutionException("Operação de backup interrompida", exception);
        }
    }

    private Map<String, String> ambientePostgres() {
        URI uri;
        try {
            String url = dataSourceProperties.determineUrl();
            if (!url.startsWith("jdbc:postgresql:")) {
                throw new IllegalArgumentException("URL JDBC não é PostgreSQL");
            }
            uri = URI.create(url.substring("jdbc:".length()));
        } catch (RuntimeException exception) {
            throw new BackupExecutionException("Configuração da conexão PostgreSQL inválida", exception);
        }

        String database = uri.getPath();
        if (database == null || database.length() <= 1) {
            throw new BackupExecutionException("Configuração da conexão PostgreSQL inválida");
        }

        return Map.of(
                "PGHOST", uri.getHost() == null ? "localhost" : uri.getHost(),
                "PGPORT", String.valueOf(uri.getPort() == -1 ? 5432 : uri.getPort()),
                "PGDATABASE", database.substring(1),
                "PGUSER", dataSourceProperties.determineUsername(),
                "PGPASSWORD", dataSourceProperties.determinePassword()
        );
    }

    private String banco() {
        String url = dataSourceProperties.determineUrl();
        return url.substring("jdbc:".length());
    }
}
