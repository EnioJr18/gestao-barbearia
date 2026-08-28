package br.com.barbeirofinanceiro.application.despesa;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento; import br.com.barbeirofinanceiro.domain.despesarecorrente.Periodicidade;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record CriarDespesaRecorrenteRequest(@NotBlank(message="descricao é obrigatória") String descricao,
 @NotNull @DecimalMin(value="0.01",message="valor deve ser maior que zero") BigDecimal valor,@NotNull UUID categoriaId,
 @NotNull @Min(value=1) @Max(value=31) Short diaVencimento,@NotNull Periodicidade periodicidade,@NotNull FormaPagamento formaPagamento,
 @NotNull LocalDate dataInicio,LocalDate dataFim) {}
