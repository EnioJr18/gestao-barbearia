package br.com.barbeirofinanceiro.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FaturamentoDiarioResponse(LocalDate data, BigDecimal faturamento) {
}
