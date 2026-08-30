package br.com.barbeirofinanceiro.application.relatorio;

import java.math.BigDecimal;
import java.util.UUID;

public record ClienteRelatorioResponse(UUID clienteId, String nome, long quantidadeVendas, BigDecimal valorTotal) {}
