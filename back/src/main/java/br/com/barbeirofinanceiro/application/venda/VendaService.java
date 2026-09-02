package br.com.barbeirofinanceiro.application.venda;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.cliente.Cliente;
import br.com.barbeirofinanceiro.domain.cliente.ClienteRepository;
import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.ItemRepository;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
import br.com.barbeirofinanceiro.domain.venda.ItemVenda;
import br.com.barbeirofinanceiro.domain.venda.ItemVendaRepository;
import br.com.barbeirofinanceiro.domain.venda.StatusVenda;
import br.com.barbeirofinanceiro.domain.venda.Venda;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamento;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamentoRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VendaService {

    private final VendaRepository vendaRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final VendaPagamentoRepository pagamentoRepository;
    private final ItemRepository itemRepository;
    private final ClienteRepository clienteRepository;
    private final CaixaRepository caixaRepository;

    public VendaService(
            VendaRepository vendaRepository,
            ItemVendaRepository itemVendaRepository,
            VendaPagamentoRepository pagamentoRepository,
            ItemRepository itemRepository,
            ClienteRepository clienteRepository,
            CaixaRepository caixaRepository
    ) {
        this.vendaRepository = vendaRepository;
        this.itemVendaRepository = itemVendaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.itemRepository = itemRepository;
        this.clienteRepository = clienteRepository;
        this.caixaRepository = caixaRepository;
    }

    @Transactional
    public Venda criar(CriarVendaRequest request) {
        return criarVenda(request);
    }

    @Transactional
    public VendaResponse criarComResposta(CriarVendaRequest request) {
        return resposta(criarVenda(request));
    }

    private Venda criarVenda(CriarVendaRequest request) {
        Caixa caixa = caixaRepository.findOpenForUpdate()
                .orElseThrow(() -> new VendaConflictException("Não há caixa aberto"));
        Cliente cliente = request.clienteId() == null
                ? null
                : clienteRepository.findById(request.clienteId())
                        .orElseThrow(() -> new VendaNotFoundException("Cliente não encontrado"));

        if (request.itens().stream().map(CriarVendaRequest.ItemRequest::itemId).distinct().count()
                != request.itens().size()) {
            throw new VendaValidationException("Não é permitido repetir o mesmo item na venda");
        }

        Venda venda = new Venda();
        venda.setCaixa(caixa);
        venda.setCliente(cliente);
        venda.setDataVenda(LocalDate.now());
        venda.setStatus(StatusVenda.FINALIZADA);

        BigDecimal total = BigDecimal.ZERO;
        List<ItemVenda> linhas = new ArrayList<>();

        for (CriarVendaRequest.ItemRequest itemRequest : request.itens()) {
            if (itemRequest.quantidade() == null || itemRequest.quantidade() <= 0) {
                throw new VendaValidationException("quantidade deve ser maior que zero");
            }

            Item item = itemRepository.findByIdForUpdate(itemRequest.itemId())
                    .orElseThrow(() -> new VendaNotFoundException("Item não encontrado"));

            if (!item.isAtivo()) {
                throw new VendaConflictException("Item inativo não pode ser vendido");
            }

            if (item.getTipo() == TipoItem.PRODUTO) {
                if (item.getEstoque() == null || item.getEstoque() < itemRequest.quantidade()) {
                    throw new VendaConflictException("Estoque insuficiente");
                }
                item.setEstoque(item.getEstoque() - itemRequest.quantidade());
                itemRepository.save(item);
            }

            BigDecimal subtotal = item.getPreco().multiply(BigDecimal.valueOf(itemRequest.quantidade()));
            total = total.add(subtotal);

            ItemVenda linha = new ItemVenda();
            linha.setVenda(venda);
            linha.setItem(item);
            linha.setQuantidade(itemRequest.quantidade());
            linha.setPrecoUnitario(item.getPreco());
            linha.setSubtotal(subtotal);
            linhas.add(linha);
        }

        if (total.signum() <= 0) {
            throw new VendaValidationException("valor total deve ser maior que zero");
        }

        validarPagamentos(request.pagamentos(), total);

        venda.setValorTotal(total);
        venda = vendaRepository.saveAndFlush(venda);

        linhas.forEach(itemVendaRepository::save);
        itemVendaRepository.flush();

        for (CriarVendaRequest.PagamentoRequest pagamentoRequest : request.pagamentos()) {
            VendaPagamento pagamento = new VendaPagamento();
            pagamento.setVenda(venda);
            pagamento.setFormaPagamento(pagamentoRequest.formaPagamento());
            pagamento.setValor(pagamentoRequest.valor());
            pagamentoRepository.save(pagamento);
        }

        pagamentoRepository.flush();
        return venda;
    }

    @Transactional
    public Venda cancelar(UUID id) {
        return cancelarVenda(id);
    }

    @Transactional
    public VendaResponse cancelarComResposta(UUID id) {
        return resposta(cancelarVenda(id));
    }

    private Venda cancelarVenda(UUID id) {
        Venda venda = carregar(id);
        if (venda.getStatus() != StatusVenda.FINALIZADA) {
            throw new VendaConflictException("Venda já está cancelada");
        }

        for (ItemVenda linha : itemVendaRepository.findByVendaId(id)) {
            Item item = itemRepository.findByIdForUpdate(linha.getItem().getId())
                    .orElseThrow(() -> new VendaNotFoundException("Item não encontrado"));
            if (item.getTipo() == TipoItem.PRODUTO) {
                item.setEstoque((item.getEstoque() == null ? 0 : item.getEstoque()) + linha.getQuantidade());
                itemRepository.save(item);
            }
        }

        venda.setStatus(StatusVenda.CANCELADA);
        return vendaRepository.saveAndFlush(venda);
    }

    @Transactional(readOnly = true)
    public Venda carregar(UUID id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new VendaNotFoundException("Venda não encontrada"));
    }

    @Transactional(readOnly = true)
    public VendaResponse buscarComResposta(UUID id) {
        return resposta(carregar(id));
    }

    @Transactional(readOnly = true)
    public List<ItemVenda> itens(UUID id) {
        return itemVendaRepository.findByVendaId(id);
    }

    @Transactional(readOnly = true)
    public List<VendaPagamento> pagamentos(UUID id) {
        return pagamentoRepository.findByVendaId(id);
    }

    @Transactional(readOnly = true)
    public List<Venda> listar(
            LocalDate dataInicial,
            LocalDate dataFinal,
            UUID cliente,
            StatusVenda status
    ) {
        Specification<Venda> specification = (root, query, builder) -> builder.conjunction();

        if (dataInicial != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("dataVenda"), dataInicial));
        }
        if (dataFinal != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("dataVenda"), dataFinal));
        }
        if (cliente != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("cliente").get("id"), cliente));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }

        return vendaRepository.findAll(
                specification,
                Sort.by(Sort.Direction.DESC, "dataVenda", "createdAt")
        );
    }

    @Transactional(readOnly = true)
    public List<VendaResponse> listarRespostas(
            LocalDate dataInicial,
            LocalDate dataFinal,
            UUID cliente,
            StatusVenda status
    ) {
        List<Venda> vendas = listar(dataInicial, dataFinal, cliente, status);
        if (vendas.isEmpty()) {
            return List.of();
        }

        List<UUID> vendaIds = vendas.stream().map(Venda::getId).toList();
        Map<UUID, List<ItemVenda>> itensPorVenda = itemVendaRepository.findByVendaIdIn(vendaIds)
                .stream()
                .collect(Collectors.groupingBy(itemVenda -> itemVenda.getVenda().getId()));
        Map<UUID, List<VendaPagamento>> pagamentosPorVenda = pagamentoRepository.findByVendaIdIn(vendaIds)
                .stream()
                .collect(Collectors.groupingBy(pagamento -> pagamento.getVenda().getId()));

        return vendas.stream()
                .map(venda -> VendaResponse.from(
                        venda,
                        itensPorVenda.getOrDefault(venda.getId(), List.of()),
                        pagamentosPorVenda.getOrDefault(venda.getId(), List.of())
                ))
                .toList();
    }

    private VendaResponse resposta(Venda venda) {
        return VendaResponse.from(venda, itens(venda.getId()), pagamentos(venda.getId()));
    }

    private void validarPagamentos(
            List<CriarVendaRequest.PagamentoRequest> pagamentos,
            BigDecimal total
    ) {
        if (pagamentos.stream().anyMatch(pagamento ->
                pagamento.valor() == null || pagamento.valor().signum() <= 0)) {
            throw new VendaValidationException("valor do pagamento deve ser maior que zero");
        }

        BigDecimal soma = pagamentos.stream()
                .map(CriarVendaRequest.PagamentoRequest::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (soma.compareTo(total) != 0) {
            throw new VendaValidationException(
                    "A soma dos pagamentos deve ser igual ao total da venda"
            );
        }
    }
}
