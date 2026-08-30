package br.com.barbeirofinanceiro.domain.movimentacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface MovimentacaoRepository
        extends JpaRepository<Movimentacao, UUID>, JpaSpecificationExecutor<Movimentacao> {

    @Query("""
        SELECT COALESCE(SUM(m.valor), 0)
        FROM Movimentacao m
        WHERE m.caixa.id = :caixaId
          AND m.tipo = br.com.barbeirofinanceiro.domain.movimentacao.TipoMovimentacao.DESPESA
          AND m.formaPagamento =
              br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento.DINHEIRO
        """)
    BigDecimal sumDespesasDinheiroByCaixaId(
            @Param("caixaId") UUID caixaId
    );

    @Query("""
        SELECT new br.com.barbeirofinanceiro.domain.movimentacao.DespesaResumoProjection(
            COALESCE(SUM(m.valor), 0)
        )
        FROM Movimentacao m
        WHERE m.tipo =
              br.com.barbeirofinanceiro.domain.movimentacao.TipoMovimentacao.DESPESA
          AND m.dataMovimentacao BETWEEN :dataInicial AND :dataFinal
        """)
    DespesaResumoProjection buscarResumoDespesas(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal
    );
}