package br.com.barbeirofinanceiro.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PostgresBackupToolsIntegrationTest extends PostgresPersistenceTest {

    @Test
    void deveGerarValidarERestaurarBackupComUtilitariosPostgres17() throws Exception {
        String arquivo = "teste-" + UUID.randomUUID() + ".backup";
        String sql = "INSERT INTO backup_execucoes (tipo, status, arquivo, inicio_em) "
                + "VALUES ('MANUAL', 'SUCESSO', '" + arquivo + "', CURRENT_TIMESTAMP)";

        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }

        var dump = POSTGRES.execInContainer(
                "sh", "-c",
                "PGPASSWORD=barbeiro pg_dump -Fc --no-owner --no-privileges -h localhost -U barbeiro "
                        + "-d barbeiro_financeiro_test -f /tmp/teste.backup"
        );
        assertEquals(0, dump.getExitCode());

        var listagem = POSTGRES.execInContainer("pg_restore", "--list", "/tmp/teste.backup");
        assertEquals(0, listagem.getExitCode());
        assertTrue(listagem.getStdout().contains("backup_execucoes"));

        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM backup_execucoes WHERE arquivo = '" + arquivo + "'");
        }

        var restore = POSTGRES.execInContainer(
                "sh", "-c",
                "PGPASSWORD=barbeiro pg_restore --clean --if-exists --no-owner --no-privileges --single-transaction "
                        + "-h localhost -U barbeiro -d barbeiro_financeiro_test /tmp/teste.backup"
        );
        assertEquals(0, restore.getExitCode());

        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT count(*) FROM backup_execucoes WHERE arquivo = '" + arquivo + "'")) {
            resultSet.next();
            assertEquals(1, resultSet.getInt(1));
        }
    }
}
