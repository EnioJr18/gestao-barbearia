package br.com.barbeirofinanceiro.application.usuario;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Serializes changes that could create or remove the last active administrator. */
@Component
public class UsuarioAdminLock {

    private static final long LOCK_ID = 741_205_150_01L;

    private final JdbcTemplate jdbcTemplate;

    public UsuarioAdminLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void bloquearAlteracoesAdministrativas() {
        jdbcTemplate.query("select pg_advisory_xact_lock(?)", resultSet -> null, LOCK_ID);
    }
}
