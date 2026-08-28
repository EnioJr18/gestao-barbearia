package br.com.barbeirofinanceiro.domain.movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, UUID>, JpaSpecificationExecutor<Movimentacao> {
    @Query("select coalesce(sum(m.valor), 0) from Movimentacao m where m.caixa.id = :caixaId and m.tipo = br.com.barbeirofinanceiro.domain.movimentacao.TipoMovimentacao.DESPESA and m.formaPagamento = br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento.DINHEIRO")
    BigDecimal sumDespesasDinheiroByCaixaId(@Param("caixaId") UUID caixaId);
}
