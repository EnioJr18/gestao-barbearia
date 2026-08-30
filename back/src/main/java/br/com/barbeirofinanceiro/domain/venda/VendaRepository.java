package br.com.barbeirofinanceiro.domain.venda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

public interface VendaRepository extends JpaRepository<Venda, UUID>, JpaSpecificationExecutor<Venda> {

    @Query("""
        SELECT new br.com.barbeirofinanceiro.domain.venda.VendaResumoProjection(
            COALESCE(SUM(v.valorTotal), 0),
            COUNT(v)
        )
        FROM Venda v
        WHERE v.status = br.com.barbeirofinanceiro.domain.venda.StatusVenda.FINALIZADA
          AND v.dataVenda BETWEEN :dataInicial AND :dataFinal
        """)
    VendaResumoProjection findResumoByDataVendaBetween(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal
    );

    @Query("""
        SELECT new br.com.barbeirofinanceiro.domain.venda.ClienteRelatorioProjection(
            v.cliente.id, v.cliente.nome, COUNT(v), SUM(v.valorTotal)
        )
        FROM Venda v
        WHERE v.status = br.com.barbeirofinanceiro.domain.venda.StatusVenda.FINALIZADA
          AND v.cliente IS NOT NULL
          AND v.dataVenda BETWEEN :dataInicial AND :dataFinal
        GROUP BY v.cliente.id, v.cliente.nome
        ORDER BY SUM(v.valorTotal) DESC
        """)
    List<ClienteRelatorioProjection> buscarRankingClientes(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal
    );
}
