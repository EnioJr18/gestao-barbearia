package br.com.barbeirofinanceiro.domain.venda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, UUID> {

    List<ItemVenda> findByVendaId(UUID vendaId);

    @Query("""
        SELECT new br.com.barbeirofinanceiro.domain.venda.ItemRelatorioProjection(
            iv.item.id,
            iv.item.nome,
            iv.item.tipo,
            SUM(iv.quantidade),
            SUM(iv.subtotal)
        )
        FROM ItemVenda iv
        WHERE iv.venda.status =
              br.com.barbeirofinanceiro.domain.venda.StatusVenda.FINALIZADA
          AND iv.venda.dataVenda BETWEEN :dataInicial AND :dataFinal
        GROUP BY iv.item.id, iv.item.nome, iv.item.tipo
        ORDER BY SUM(iv.subtotal) DESC
        """)
    List<ItemRelatorioProjection> buscarResumoItens(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal
    );
}