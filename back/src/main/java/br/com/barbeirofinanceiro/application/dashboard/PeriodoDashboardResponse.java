package br.com.barbeirofinanceiro.application.dashboard;

import java.time.LocalDate;

public record PeriodoDashboardResponse(LocalDate dataInicial, LocalDate dataFinal) {
}
