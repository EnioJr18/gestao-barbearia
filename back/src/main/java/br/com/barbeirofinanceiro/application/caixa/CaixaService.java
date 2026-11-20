package br.com.barbeirofinanceiro.application.caixa;

import br.com.barbeirofinanceiro.application.common.PageResponse;
import br.com.barbeirofinanceiro.application.common.PageResponseMapper;
import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import br.com.barbeirofinanceiro.domain.movimentacao.MovimentacaoRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamentoRepository;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Service
public class CaixaService {
    private static final int MAX_PAGE_SIZE = 100;
    private final CaixaRepository caixaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final VendaPagamentoRepository vendaPagamentoRepository;

    public CaixaService(CaixaRepository caixaRepository, UsuarioRepository usuarioRepository,
                        MovimentacaoRepository movimentacaoRepository, VendaPagamentoRepository vendaPagamentoRepository) {
        this.caixaRepository = caixaRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.vendaPagamentoRepository = vendaPagamentoRepository;
    }

    @Transactional
    public Caixa abrir(BigDecimal valorInicial, Authentication authentication) {
        return abrirCaixa(valorInicial, authentication);
    }

    @Transactional
    public CaixaResponse abrirComResposta(BigDecimal valorInicial, Authentication authentication) {
        return resposta(abrirCaixa(valorInicial, authentication));
    }

    private Caixa abrirCaixa(BigDecimal valorInicial, Authentication authentication) {
        if (valorInicial == null || valorInicial.signum() <= 0) {
            throw new CaixaValidationException("valorInicial deve ser maior que zero");
        }

        if (caixaRepository.findFirstByStatusOrderByDataCaixaDesc(StatusCaixa.ABERTO).isPresent()) {
            throw new CaixaConflictException("Já existe um caixa aberto");
        }

        Caixa caixa = new Caixa();
        caixa.setDataCaixa(LocalDate.now());
        caixa.setValorInicial(valorInicial);
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setUsuarioAbertura(resolveAuthenticatedUser(authentication));

        try {
            return caixaRepository.saveAndFlush(caixa);
        } catch (DataIntegrityViolationException exception) {
            throw new CaixaConflictException("Já existe um caixa aberto");
        }
    }

    @Transactional(readOnly = true)
    public Caixa atual() {
        return caixaRepository.findFirstByStatusOrderByDataCaixaDesc(StatusCaixa.ABERTO)
                .orElseThrow(() -> new CaixaNotFoundException("Nenhum caixa aberto encontrado"));
    }

    @Transactional(readOnly = true)
    public CaixaResponse atualComResposta() {
        return resposta(atual());
    }

    @Transactional(readOnly = true)
    public Optional<CaixaResponse> atualComRespostaOpcional() {
        return caixaRepository.findFirstByStatusOrderByDataCaixaDesc(StatusCaixa.ABERTO)
                .map(this::resposta);
    }

    @Transactional(readOnly = true)
    public PageResponse<CaixaResponse> listarComResposta(
            StatusCaixa status,
            LocalDate dataInicial,
            LocalDate dataFinal,
            int page,
            int size
    ) {
        validarConsultaHistorico(dataInicial, dataFinal, page, size);

        Specification<Caixa> specification = (root, query, builder) -> builder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (dataInicial != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("dataCaixa"), dataInicial));
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("dataCaixa"), dataFinal));
        }

        Sort sort = Sort.by(
                Sort.Order.desc("dataCaixa"),
                Sort.Order.desc("abertoEm"),
                Sort.Order.desc("id")
        );
        PageRequest requestedPage = PageRequest.of(page, size, sort);
        Page<Caixa> caixas = caixaRepository.findAll(specification, requestedPage);
        if (caixas.getTotalElements() == 0) {
            return PageResponseMapper.from(Page.<Caixa>empty(PageRequest.of(0, size, sort)),
                    caixa -> resposta(caixa, Map.of(), BigDecimal.ZERO));
        }
        if (page >= caixas.getTotalPages()) {
            caixas = caixaRepository.findAll(
                    specification,
                    PageRequest.of(caixas.getTotalPages() - 1, size, sort)
            );
        }

        List<Caixa> caixasDaPagina = caixas.getContent();

        List<UUID> caixaIds = caixasDaPagina.stream().map(Caixa::getId).toList();
        Map<UUID, Map<FormaPagamento, BigDecimal>> pagamentosPorCaixa = new HashMap<>();
        vendaPagamentoRepository.buscarTotaisPorCaixas(caixaIds).forEach(resumo ->
                pagamentosPorCaixa
                        .computeIfAbsent(resumo.caixaId(), ignored -> new EnumMap<>(FormaPagamento.class))
                        .put(resumo.formaPagamento(), resumo.valor())
        );
        Map<UUID, BigDecimal> despesasPorCaixa = new HashMap<>();
        movimentacaoRepository.buscarDespesasDinheiroPorCaixas(caixaIds)
                .forEach(resumo -> despesasPorCaixa.put(resumo.caixaId(), resumo.valor()));

        return PageResponseMapper.from(caixas, caixa -> resposta(
                caixa,
                pagamentosPorCaixa.getOrDefault(caixa.getId(), Map.of()),
                despesasPorCaixa.getOrDefault(caixa.getId(), BigDecimal.ZERO)
        ));
    }

    private void validarConsultaHistorico(
            LocalDate dataInicial,
            LocalDate dataFinal,
            int page,
            int size
    ) {
        if ((dataInicial == null) != (dataFinal == null)) {
            throw new CaixaValidationException("dataInicial e dataFinal devem ser informadas juntas");
        }
        if (dataInicial != null && dataInicial.isAfter(dataFinal)) {
            throw new CaixaValidationException("dataInicial deve ser anterior ou igual a dataFinal");
        }
        if (page < 0) {
            throw new CaixaValidationException("page deve ser maior ou igual a zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new CaixaValidationException("size deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
    }

    @Transactional
    public Caixa fechar(UUID id, BigDecimal valorApurado, Authentication authentication) {
        return fecharCaixa(id, valorApurado, authentication);
    }

    @Transactional
    public CaixaResponse fecharComResposta(
            UUID id,
            BigDecimal valorApurado,
            Authentication authentication
    ) {
        return resposta(fecharCaixa(id, valorApurado, authentication));
    }

    private Caixa fecharCaixa(UUID id, BigDecimal valorApurado, Authentication authentication) {
        if (valorApurado == null || valorApurado.signum() < 0) {
            throw new CaixaValidationException("valorApurado deve ser maior ou igual a zero");
        }

        Caixa caixa = caixaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new CaixaNotFoundException("Caixa não encontrado"));
        if (caixa.getStatus() != StatusCaixa.ABERTO) {
            throw new CaixaConflictException("Caixa já está fechado");
        }

        caixa.setValorApurado(valorApurado);
        caixa.setDiferenca(valorApurado.subtract(valorEsperado(caixa)));
        caixa.setStatus(StatusCaixa.FECHADO);
        caixa.setUsuarioFechamento(resolveAuthenticatedUser(authentication));
        caixa.setFechadoEm(Instant.now());
        return caixaRepository.saveAndFlush(caixa);
    }

    public BigDecimal entradasDinheiro(Caixa caixa) {
        return vendaPagamentoRepository.sumDinheiroByCaixaId(caixa.getId());
    }

    public BigDecimal saidasDinheiro(Caixa caixa) {
        return movimentacaoRepository.sumDespesasDinheiroByCaixaId(caixa.getId());
    }

    public BigDecimal valorEsperado(Caixa caixa) {
        return caixa.getValorInicial().add(entradasDinheiro(caixa)).subtract(saidasDinheiro(caixa));
    }

    private CaixaResponse resposta(Caixa caixa) {
        BigDecimal dinheiro = entradasDinheiro(caixa);
        BigDecimal saidas = saidasDinheiro(caixa);
        BigDecimal pix = vendaPagamentoRepository.sumByCaixaIdAndFormaPagamento(
                caixa.getId(), FormaPagamento.PIX);
        BigDecimal debito = vendaPagamentoRepository.sumByCaixaIdAndFormaPagamento(
                caixa.getId(), FormaPagamento.CARTAO_DEBITO);
        BigDecimal credito = vendaPagamentoRepository.sumByCaixaIdAndFormaPagamento(
                caixa.getId(), FormaPagamento.CARTAO_CREDITO);
        return resposta(caixa, Map.of(
                FormaPagamento.DINHEIRO, dinheiro,
                FormaPagamento.PIX, pix,
                FormaPagamento.CARTAO_DEBITO, debito,
                FormaPagamento.CARTAO_CREDITO, credito
        ), saidas);
    }

    private CaixaResponse resposta(
            Caixa caixa,
            Map<FormaPagamento, BigDecimal> pagamentos,
            BigDecimal saidas
    ) {
        BigDecimal dinheiro = pagamentos.getOrDefault(FormaPagamento.DINHEIRO, BigDecimal.ZERO);
        BigDecimal pix = pagamentos.getOrDefault(FormaPagamento.PIX, BigDecimal.ZERO);
        BigDecimal debito = pagamentos.getOrDefault(FormaPagamento.CARTAO_DEBITO, BigDecimal.ZERO);
        BigDecimal credito = pagamentos.getOrDefault(FormaPagamento.CARTAO_CREDITO, BigDecimal.ZERO);
        BigDecimal valorEsperado = caixa.getStatus() == StatusCaixa.FECHADO
                ? caixa.getValorApurado().subtract(caixa.getDiferenca())
                : caixa.getValorInicial().add(dinheiro).subtract(saidas);
        return new CaixaResponse(
                caixa.getId(),
                caixa.getDataCaixa(),
                caixa.getValorInicial(),
                dinheiro,
                saidas,
                dinheiro,
                pix,
                debito,
                credito,
                valorEsperado,
                caixa.getValorApurado(),
                caixa.getDiferenca(),
                caixa.getStatus(),
                caixa.getUsuarioAbertura().getId(),
                caixa.getUsuarioFechamento() == null ? null : caixa.getUsuarioFechamento().getId(),
                caixa.getAbertoEm(),
                caixa.getFechadoEm(),
                caixa.getCreatedAt(),
                caixa.getUpdatedAt()
        );
    }

    private Usuario resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticatedUserException("Usuário autenticado é obrigatório");
        }

        String name = authentication.getName();
        try {
            return usuarioRepository.findById(UUID.fromString(name))
                    .orElseThrow(() -> new AuthenticatedUserException("Usuário autenticado não encontrado"));
        } catch (IllegalArgumentException ignored) {
            return usuarioRepository.findByEmailIgnoreCase(name)
                    .orElseThrow(() -> new AuthenticatedUserException("Usuário autenticado não encontrado"));
        }
    }
}
