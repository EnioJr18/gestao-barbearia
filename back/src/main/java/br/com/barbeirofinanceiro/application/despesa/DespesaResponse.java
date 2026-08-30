package br.com.barbeirofinanceiro.application.despesa;
import br.com.barbeirofinanceiro.domain.movimentacao.*; import java.math.BigDecimal; import java.time.*; import java.util.UUID;
public record DespesaResponse(UUID id,String descricao,BigDecimal valor,LocalDate dataMovimentacao,UUID categoriaId,FormaPagamento formaPagamento,UUID caixaId,String observacao,Instant createdAt){
 public static DespesaResponse from(Movimentacao m){return new DespesaResponse(m.getId(),m.getDescricao(),m.getValor(),m.getDataMovimentacao(),m.getCategoria().getId(),m.getFormaPagamento(),m.getCaixa()==null?null:m.getCaixa().getId(),m.getObservacao(),m.getCreatedAt());}}
