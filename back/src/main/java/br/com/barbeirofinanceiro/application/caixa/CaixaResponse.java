package br.com.barbeirofinanceiro.application.caixa;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CaixaResponse(
        UUID id,
        LocalDate dataCaixa,
        BigDecimal valorInicial,
        BigDecimal entradasDinheiro,
        BigDecimal saidasDinheiro,
        BigDecimal valorEsperado,
        BigDecimal valorApurado,
        BigDecimal diferenca,
        StatusCaixa status,
        UUID usuarioAberturaId,
        UUID usuarioFechamentoId,
        Instant abertoEm,
        Instant fechadoEm,
        Instant createdAt,
        Instant updatedAt
) {
    public static CaixaResponse from(Caixa caixa) {
        return from(caixa, BigDecimal.ZERO, BigDecimal.ZERO, caixa.getValorInicial());
    }

    public static CaixaResponse from(Caixa caixa, BigDecimal entradasDinheiro,
                                     BigDecimal saidasDinheiro, BigDecimal valorEsperado) {
        return new CaixaResponse(
                caixa.getId(),
                caixa.getDataCaixa(),
                caixa.getValorInicial(),
                entradasDinheiro,
                saidasDinheiro,
                valorEsperado,
                caixa.getValorApurado(),
                caixa.getDiferenca(),
                caixa.getStatus(),
                caixa.getUsuarioAbertura().getId(),
                caixa.getUsuarioFechamento() == null ? null : caixa.getUsuarioFechamento().getId(),
                caixa.getAbertoEm(),
                caixa.getFechadoEm(),
                caixa.getCreatedAt(),
                caixa.getUpdatedAt()
        );
    }
}
