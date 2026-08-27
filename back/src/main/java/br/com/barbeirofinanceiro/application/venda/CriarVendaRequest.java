package br.com.barbeirofinanceiro.application.venda;

import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CriarVendaRequest(UUID clienteId, @NotEmpty List<@Valid ItemRequest> itens,
                                @NotEmpty List<@Valid PagamentoRequest> pagamentos) {
    public record ItemRequest(@NotNull UUID itemId, @NotNull Integer quantidade) {}
    public record PagamentoRequest(@NotNull FormaPagamento formaPagamento, @NotNull BigDecimal valor) {}
}
