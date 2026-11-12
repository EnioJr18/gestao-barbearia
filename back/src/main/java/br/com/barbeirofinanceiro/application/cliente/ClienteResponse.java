package br.com.barbeirofinanceiro.application.cliente;

import br.com.barbeirofinanceiro.domain.cliente.Cliente;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClienteResponse(
        UUID id,
        String nome,
        String telefone,
        boolean ativo,
        Instant createdAt,
    Instant updatedAt,
    long visitas,
    BigDecimal totalGasto,
    LocalDate ultimaVisita
) {
public static ClienteResponse from(Cliente cliente, ClienteResumoProjection resumo) {
    return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getTelefone(),
            cliente.isAtivo(), cliente.getCreatedAt(), cliente.getUpdatedAt(),
            resumo.visitas(), resumo.totalGasto(), resumo.ultimaVisita());
}
}
