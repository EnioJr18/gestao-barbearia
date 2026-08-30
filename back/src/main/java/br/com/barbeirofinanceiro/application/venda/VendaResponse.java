package br.com.barbeirofinanceiro.application.venda;

import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import br.com.barbeirofinanceiro.domain.venda.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VendaResponse(UUID id, UUID clienteId, UUID caixaId, LocalDate dataVenda,
                            BigDecimal valorTotal, StatusVenda status, Instant createdAt,
                            List<ItemResponse> itens, List<PagamentoResponse> pagamentos) {
    public record ItemResponse(UUID itemId, Integer quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {}
    public record PagamentoResponse(FormaPagamento formaPagamento, BigDecimal valor, Instant createdAt) {}

    public static VendaResponse from(Venda venda, List<ItemVenda> itens, List<VendaPagamento> pagamentos) {
        return new VendaResponse(venda.getId(), venda.getCliente() == null ? null : venda.getCliente().getId(),
                venda.getCaixa().getId(), venda.getDataVenda(), venda.getValorTotal(), venda.getStatus(),
                venda.getCreatedAt(), itens.stream().map(i -> new ItemResponse(i.getItem().getId(), i.getQuantidade(),
                        i.getPrecoUnitario(), i.getSubtotal())).toList(), pagamentos.stream()
                        .map(p -> new PagamentoResponse(p.getFormaPagamento(), p.getValor(), p.getCreatedAt())).toList());
    }
}
