package br.com.barbeirofinanceiro.application.cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ClienteResumoProjection(
        UUID clienteId,
        long visitas,
        BigDecimal totalGasto,
        LocalDate ultimaVisita
) {}
