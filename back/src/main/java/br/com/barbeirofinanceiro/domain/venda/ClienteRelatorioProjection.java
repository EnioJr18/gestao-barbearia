package br.com.barbeirofinanceiro.domain.venda;

import java.math.BigDecimal;
import java.util.UUID;

public record ClienteRelatorioProjection(UUID clienteId, String nome, long quantidadeVendas, BigDecimal valorTotal) {}
