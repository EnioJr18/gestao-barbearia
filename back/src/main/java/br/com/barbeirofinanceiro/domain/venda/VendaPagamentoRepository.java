package br.com.barbeirofinanceiro.domain.venda;

import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VendaPagamentoRepository extends JpaRepository<VendaPagamento, UUID> {

    List<VendaPagamento> findByVendaId(UUID vendaId);

    List<VendaPagamento> findByVendaIdIn(Collection<UUID> vendaIds);

    @Query("""
        SELECT COALESCE(SUM(p.valor), 0)
        FROM VendaPagamento p
        WHERE p.venda.caixa.id = :caixaId
          AND p.venda.status = br.com.barbeirofinanceiro.domain.venda.StatusVenda.FINALIZADA
          AND p.formaPagamento =
              br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento.DINHEIRO
        """)
    BigDecimal sumDinheiroByCaixaId(@Param("caixaId") UUID caixaId);

    @Query("""
        SELECT new br.com.barbeirofinanceiro.domain.venda.PagamentoResumoProjection(
            p.formaPagamento,
            COALESCE(SUM(p.valor), 0)
        )
        FROM VendaPagamento p
        WHERE p.venda.status =
              br.com.barbeirofinanceiro.domain.venda.StatusVenda.FINALIZADA
          AND p.venda.dataVenda BETWEEN :dataInicial AND :dataFinal
        GROUP BY p.formaPagamento
        """)
    List<PagamentoResumoProjection> buscarResumoPorFormaPagamento(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal
    );
}
