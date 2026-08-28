package br.com.barbeirofinanceiro.domain.venda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public interface VendaPagamentoRepository extends JpaRepository<VendaPagamento, UUID> {
    List<VendaPagamento> findByVendaId(UUID vendaId);

    @Query("select coalesce(sum(p.valor), 0) from VendaPagamento p where p.venda.caixa.id = :caixaId and p.venda.status = br.com.barbeirofinanceiro.domain.venda.StatusVenda.FINALIZADA and p.formaPagamento = br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento.DINHEIRO")
    BigDecimal sumDinheiroByCaixaId(@Param("caixaId") UUID caixaId);
}
