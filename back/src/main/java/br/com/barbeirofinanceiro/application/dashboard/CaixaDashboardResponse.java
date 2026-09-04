package br.com.barbeirofinanceiro.application.dashboard;

import br.com.barbeirofinanceiro.application.caixa.CaixaResponse;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;

import java.math.BigDecimal;
import java.util.UUID;

public record CaixaDashboardResponse(
        UUID id,
        StatusCaixa status,
        BigDecimal valorInicial,
        BigDecimal entradasDinheiro,
        BigDecimal saidasDinheiro,
        BigDecimal valorEsperado,
        BigDecimal valorApurado,
        BigDecimal diferenca
) {
    public static CaixaDashboardResponse from(CaixaResponse caixa) {
        return new CaixaDashboardResponse(
                caixa.id(), caixa.status(), caixa.valorInicial(), caixa.entradasDinheiro(),
                caixa.saidasDinheiro(), caixa.valorEsperado(), caixa.valorApurado(), caixa.diferenca()
        );
    }
}
