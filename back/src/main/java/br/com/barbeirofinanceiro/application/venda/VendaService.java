package br.com.barbeirofinanceiro.application.venda;

import br.com.barbeirofinanceiro.domain.caixa.*;
import br.com.barbeirofinanceiro.domain.cliente.*;
import br.com.barbeirofinanceiro.domain.item.*;
import br.com.barbeirofinanceiro.domain.venda.*;
import br.com.barbeirofinanceiro.domain.venda.ItemVendaRepository;
import br.com.barbeirofinanceiro.domain.venda.ItemRelatorioProjection;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class VendaService {
    private final VendaRepository vendaRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final VendaPagamentoRepository pagamentoRepository;
    private final ItemRepository itemRepository;
    private final ClienteRepository clienteRepository;
    private final CaixaRepository caixaRepository;

    public VendaService(VendaRepository v, ItemVendaRepository iv, VendaPagamentoRepository p,
                        ItemRepository i, ClienteRepository c, CaixaRepository caixa) {
        vendaRepository=v; itemVendaRepository=iv; pagamentoRepository=p; itemRepository=i; clienteRepository=c; caixaRepository=caixa;
    }

    @Transactional
    public Venda criar(CriarVendaRequest request) {
        Caixa caixa = caixaRepository.findOpenForUpdate().orElseThrow(() -> new VendaConflictException("Não há caixa aberto"));
        Cliente cliente = request.clienteId() == null ? null : clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new VendaNotFoundException("Cliente não encontrado"));
        if (request.itens().stream().map(CriarVendaRequest.ItemRequest::itemId).distinct().count() != request.itens().size())
            throw new VendaValidationException("Não é permitido repetir o mesmo item na venda");
        Venda venda = new Venda(); venda.setCaixa(caixa); venda.setCliente(cliente); venda.setDataVenda(LocalDate.now());
        venda.setStatus(StatusVenda.FINALIZADA);
        BigDecimal total = BigDecimal.ZERO;
        List<ItemVenda> linhas = new ArrayList<>();
        for (var req : request.itens()) {
            if (req.quantidade() == null || req.quantidade() <= 0) throw new VendaValidationException("quantidade deve ser maior que zero");
            Item item = itemRepository.findByIdForUpdate(req.itemId()).orElseThrow(() -> new VendaNotFoundException("Item não encontrado"));
            if (!item.isAtivo()) throw new VendaConflictException("Item inativo não pode ser vendido");
            if (item.getTipo() == TipoItem.PRODUTO) {
                if (item.getEstoque() == null || item.getEstoque() < req.quantidade()) throw new VendaConflictException("Estoque insuficiente");
                item.setEstoque(item.getEstoque() - req.quantidade());
                itemRepository.save(item);
            }
            BigDecimal subtotal = item.getPreco().multiply(BigDecimal.valueOf(req.quantidade())); total = total.add(subtotal);
            ItemVenda linha = new ItemVenda(); linha.setVenda(venda); linha.setItem(item); linha.setQuantidade(req.quantidade());
            linha.setPrecoUnitario(item.getPreco()); linha.setSubtotal(subtotal); linhas.add(linha);
        }
        if (total.signum() <= 0) throw new VendaValidationException("valor total deve ser maior que zero");
        validarPagamentos(request.pagamentos(), total);
        venda.setValorTotal(total); venda = vendaRepository.saveAndFlush(venda);
        linhas.forEach(itemVendaRepository::save); itemVendaRepository.flush();
        for (var req : request.pagamentos()) { VendaPagamento p = new VendaPagamento(); p.setVenda(venda); p.setFormaPagamento(req.formaPagamento()); p.setValor(req.valor()); pagamentoRepository.save(p); }
        pagamentoRepository.flush(); return venda;
    }

    @Transactional
    public Venda cancelar(UUID id) {
        Venda venda = carregar(id);
        if (venda.getStatus() != StatusVenda.FINALIZADA) throw new VendaConflictException("Venda já está cancelada");
        for (ItemVenda linha : itemVendaRepository.findByVendaId(id)) {
            Item item = itemRepository.findByIdForUpdate(linha.getItem().getId()).orElseThrow(() -> new VendaNotFoundException("Item não encontrado"));
            if (item.getTipo() == TipoItem.PRODUTO) { item.setEstoque((item.getEstoque() == null ? 0 : item.getEstoque()) + linha.getQuantidade()); itemRepository.save(item); }
        }
        venda.setStatus(StatusVenda.CANCELADA); return vendaRepository.saveAndFlush(venda);
    }

    @Transactional(readOnly=true)
    public Venda carregar(UUID id) { return vendaRepository.findById(id).orElseThrow(() -> new VendaNotFoundException("Venda não encontrada")); }
    @Transactional(readOnly=true)
    public List<ItemVenda> itens(UUID id) { return itemVendaRepository.findByVendaId(id); }
    @Transactional(readOnly=true)
    public List<VendaPagamento> pagamentos(UUID id) { return pagamentoRepository.findByVendaId(id); }
    @Transactional(readOnly=true)
    public List<Venda> listar(LocalDate inicial, LocalDate finalDate, UUID cliente, StatusVenda status) {
        Specification<Venda> s=(r,q,b)->b.conjunction();
        if(inicial!=null)s=s.and((r,q,b)->b.greaterThanOrEqualTo(r.get("dataVenda"),inicial));
        if(finalDate!=null)s=s.and((r,q,b)->b.lessThanOrEqualTo(r.get("dataVenda"),finalDate));
        if(cliente!=null)s=s.and((r,q,b)->b.equal(r.get("cliente").get("id"),cliente));
        if(status!=null)s=s.and((r,q,b)->b.equal(r.get("status"),status));
        return vendaRepository.findAll(s,Sort.by(Sort.Direction.DESC,"dataVenda","createdAt"));
    }
    private void validarPagamentos(List<CriarVendaRequest.PagamentoRequest> ps, BigDecimal total) {
        if(ps.stream().anyMatch(p->p.valor()==null||p.valor().signum()<=0)) throw new VendaValidationException("valor do pagamento deve ser maior que zero");
        BigDecimal soma=ps.stream().map(CriarVendaRequest.PagamentoRequest::valor).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(soma.compareTo(total)!=0) throw new VendaValidationException("A soma dos pagamentos deve ser igual ao total da venda");
    }
}
