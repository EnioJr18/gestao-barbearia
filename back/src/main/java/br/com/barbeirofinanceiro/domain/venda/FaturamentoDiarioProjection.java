package br.com.barbeirofinanceiro.domain.venda;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FaturamentoDiarioProjection(LocalDate data, BigDecimal faturamento) {
}
