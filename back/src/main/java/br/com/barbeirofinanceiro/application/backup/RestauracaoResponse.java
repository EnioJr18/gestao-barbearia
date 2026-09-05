package br.com.barbeirofinanceiro.application.backup;

import java.time.Instant;

public record RestauracaoResponse(
        String arquivo,
        Instant concluidaEm,
        boolean reinicioNecessario,
        String mensagem
) {
}
