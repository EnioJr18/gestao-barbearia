package br.com.barbeirofinanceiro.application.despesa;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import jakarta.validation.constraints.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record CriarMovimentacaoRequest(@NotBlank(message="descricao é obrigatória") String descricao,
 @NotNull @DecimalMin(value="0.01",message="valor deve ser maior que zero") BigDecimal valor,
 @NotNull LocalDate dataMovimentacao,@NotNull UUID categoriaId,@NotNull FormaPagamento formaPagamento,String observacao) {}
