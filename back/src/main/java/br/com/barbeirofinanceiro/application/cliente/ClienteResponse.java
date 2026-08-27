package br.com.barbeirofinanceiro.application.cliente;

import br.com.barbeirofinanceiro.domain.cliente.Cliente;

import java.time.Instant;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String nome,
        String telefone,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getTelefone(),
                cliente.isAtivo(), cliente.getCreatedAt(), cliente.getUpdatedAt());
    }
}
