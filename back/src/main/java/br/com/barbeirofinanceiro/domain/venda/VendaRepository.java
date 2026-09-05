package br.com.barbeirofinanceiro.domain.venda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

public interface VendaRepository extends JpaRepository<Venda, UUID>, JpaSpecificationExecutor<Venda> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "caixa"})
    List<Venda> findAll(Specification<Venda> specification, Sort sort);

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
        SELECT new br.com.barbeirofinanceiro.domain.venda.FaturamentoDiarioProjection(
            v.dataVenda, COALESCE(SUM(v.valorTotal), 0)
        )
        FROM Venda v
        WHERE v.status = br.com.barbeirofinanceiro.domain.venda.StatusVenda.FINALIZADA
          AND v.dataVenda BETWEEN :dataInicial AND :dataFinal
        GROUP BY v.dataVenda
        ORDER BY v.dataVenda ASC
        """)
    List<FaturamentoDiarioProjection> buscarFaturamentoDiario(
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
