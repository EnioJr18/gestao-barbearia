package br.com.barbeirofinanceiro.application.despesa;

import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import br.com.barbeirofinanceiro.domain.movimentacao.Movimentacao;
import br.com.barbeirofinanceiro.domain.movimentacao.MovimentacaoRepository;
import br.com.barbeirofinanceiro.domain.movimentacao.OrigemMovimentacao;
import br.com.barbeirofinanceiro.domain.movimentacao.TipoMovimentacao;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MovimentacaoDespesaService {

    private final MovimentacaoRepository movimentacaoRepository;
    private final CaixaRepository caixaRepository;
    private final CategoriaDespesaResolver categoriaDespesaResolver;

    public MovimentacaoDespesaService(
            MovimentacaoRepository movimentacaoRepository,
            CaixaRepository caixaRepository,
            CategoriaDespesaResolver categoriaDespesaResolver
    ) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.caixaRepository = caixaRepository;
        this.categoriaDespesaResolver = categoriaDespesaResolver;
    }

    @Transactional
    public Movimentacao criar(CriarMovimentacaoRequest request) {
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setTipo(TipoMovimentacao.DESPESA);
        movimentacao.setOrigem(OrigemMovimentacao.MANUAL);
        movimentacao.setDescricao(request.descricao().trim());
        movimentacao.setValor(request.valor());
        movimentacao.setDataMovimentacao(request.dataMovimentacao());
        movimentacao.setCategoria(categoriaDespesaResolver.resolver(request.categoriaId()));
        movimentacao.setFormaPagamento(request.formaPagamento());
        movimentacao.setObservacao(request.observacao());

        if (request.formaPagamento() == FormaPagamento.DINHEIRO) {
            caixaRepository.findOpenForUpdate().ifPresent(movimentacao::setCaixa);
        }

        return movimentacaoRepository.saveAndFlush(movimentacao);
    }

    @Transactional(readOnly = true)
    public Movimentacao buscar(UUID id) {
        return movimentacaoRepository.findById(id)
                .filter(movimentacao -> movimentacao.getTipo() == TipoMovimentacao.DESPESA)
                .orElseThrow(() -> new DespesaNotFoundException("Despesa não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Movimentacao> listar(
            LocalDate dataInicial,
            LocalDate dataFinal,
            UUID categoriaId,
            FormaPagamento formaPagamento
    ) {
        Specification<Movimentacao> specification = (root, query, builder) ->
                builder.equal(root.get("tipo"), TipoMovimentacao.DESPESA);

        if (dataInicial != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("dataMovimentacao"), dataInicial));
        }
        if (dataFinal != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("dataMovimentacao"), dataFinal));
        }
        if (categoriaId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("categoria").get("id"), categoriaId));
        }
        if (formaPagamento != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("formaPagamento"), formaPagamento));
        }

        return movimentacaoRepository.findAll(
                specification,
                Sort.by(Sort.Direction.DESC, "dataMovimentacao", "createdAt")
        );
    }
}
