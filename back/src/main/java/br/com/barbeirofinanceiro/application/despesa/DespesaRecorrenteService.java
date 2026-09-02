package br.com.barbeirofinanceiro.application.despesa;

import br.com.barbeirofinanceiro.domain.despesarecorrente.DespesaRecorrente;
import br.com.barbeirofinanceiro.domain.despesarecorrente.DespesaRecorrenteRepository;
import br.com.barbeirofinanceiro.domain.despesarecorrente.Periodicidade;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DespesaRecorrenteService {

    private final DespesaRecorrenteRepository despesaRecorrenteRepository;
    private final CategoriaDespesaResolver categoriaDespesaResolver;

    public DespesaRecorrenteService(
            DespesaRecorrenteRepository despesaRecorrenteRepository,
            CategoriaDespesaResolver categoriaDespesaResolver
    ) {
        this.despesaRecorrenteRepository = despesaRecorrenteRepository;
        this.categoriaDespesaResolver = categoriaDespesaResolver;
    }

    @Transactional
    public DespesaRecorrente criar(CriarDespesaRecorrenteRequest request) {
        validar(request);
        DespesaRecorrente despesaRecorrente = new DespesaRecorrente();
        preencher(despesaRecorrente, request);
        return despesaRecorrenteRepository.saveAndFlush(despesaRecorrente);
    }

    @Transactional(readOnly = true)
    public List<DespesaRecorrente> listar() {
        return despesaRecorrenteRepository.findAll(Sort.by(Sort.Direction.ASC, "descricao"));
    }

    @Transactional(readOnly = true)
    public DespesaRecorrente buscar(UUID id) {
        return despesaRecorrenteRepository.findById(id)
                .orElseThrow(() -> new DespesaNotFoundException("Despesa recorrente não encontrada"));
    }

    @Transactional
    public DespesaRecorrente atualizar(UUID id, CriarDespesaRecorrenteRequest request) {
        validar(request);
        DespesaRecorrente despesaRecorrente = buscar(id);
        preencher(despesaRecorrente, request);
        return despesaRecorrenteRepository.saveAndFlush(despesaRecorrente);
    }

    @Transactional
    public DespesaRecorrente alterarAtiva(UUID id, boolean ativa) {
        DespesaRecorrente despesaRecorrente = buscar(id);
        despesaRecorrente.setAtiva(ativa);
        return despesaRecorrenteRepository.saveAndFlush(despesaRecorrente);
    }

    private void validar(CriarDespesaRecorrenteRequest request) {
        if (request.diaVencimento() == null
                || request.diaVencimento() < 1
                || request.diaVencimento() > 31) {
            throw new DespesaValidationException("diaVencimento deve estar entre 1 e 31");
        }
        if (request.dataFim() != null && request.dataFim().isBefore(request.dataInicio())) {
            throw new DespesaValidationException("dataFim não pode ser anterior à dataInicio");
        }
        if (request.periodicidade() != Periodicidade.MENSAL) {
            throw new DespesaValidationException("Periodicidade deve ser MENSAL");
        }

        categoriaDespesaResolver.resolver(request.categoriaId());
    }

    private void preencher(DespesaRecorrente despesaRecorrente, CriarDespesaRecorrenteRequest request) {
        despesaRecorrente.setDescricao(request.descricao().trim());
        despesaRecorrente.setValor(request.valor());
        despesaRecorrente.setCategoria(categoriaDespesaResolver.resolver(request.categoriaId()));
        despesaRecorrente.setDiaVencimento(request.diaVencimento());
        despesaRecorrente.setPeriodicidade(request.periodicidade());
        despesaRecorrente.setFormaPagamento(request.formaPagamento());
        despesaRecorrente.setDataInicio(request.dataInicio());
        despesaRecorrente.setDataFim(request.dataFim());
    }
}
