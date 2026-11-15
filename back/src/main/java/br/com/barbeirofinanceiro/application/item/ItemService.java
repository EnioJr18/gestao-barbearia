package br.com.barbeirofinanceiro.application.item;

import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.ItemRepository;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
import br.com.barbeirofinanceiro.domain.venda.ItemVendaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class ItemService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ItemRepository itemRepository;
    private final ItemVendaRepository itemVendaRepository;

    public ItemService(ItemRepository itemRepository, ItemVendaRepository itemVendaRepository) {
        this.itemRepository = itemRepository;
        this.itemVendaRepository = itemVendaRepository;
    }

    @Transactional
    public Item criar(CriarItemRequest request) {
        return criar(request.nome(), request.preco(), request.tipo(), request.estoque());
    }

    @Transactional
    public Item criarProduto(CriarProdutoRequest request) {
        return criar(request.nome(), request.preco(), TipoItem.PRODUTO, request.estoque());
    }

    @Transactional
    public Item criarServico(CriarServicoRequest request) {
        return criar(request.nome(), request.preco(), TipoItem.SERVICO, null);
    }

    private Item criar(String nome, java.math.BigDecimal preco, TipoItem tipo, Integer estoque) {
        validarEstoque(tipo, estoque);
        validarNomeDisponivel(nome, null);

        Item item = new Item();
        item.setNome(normalizarNome(nome));
        item.setTipo(tipo);
        item.setPreco(preco);
        item.setEstoque(estoque);
        return salvar(item);
    }

    @Transactional(readOnly = true)
    public Page<Item> listar(TipoItem tipo, Boolean ativo, String nome, int page, int size) {
        validarPaginacao(page, size);

        Specification<Item> specification = (root, query, builder) -> builder.conjunction();
        if (tipo != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("tipo"), tipo));
        }
        if (ativo != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("ativo"), ativo));
        }
        if (nome != null && !nome.isBlank()) {
            String termo = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("nome")), termo));
        }
        Sort sort = Sort.by(Sort.Direction.ASC, "nome");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Item> resultado = itemRepository.findAll(specification, pageable);

        if (resultado.getTotalElements() == 0) {
            return Page.empty(PageRequest.of(0, size, sort));
        }

        if (page >= resultado.getTotalPages()) {
            int ultimaPagina = resultado.getTotalPages() - 1;
            return itemRepository.findAll(specification, PageRequest.of(ultimaPagina, size, sort));
        }

        return resultado;
    }

    @Transactional(readOnly = true)
    public Item buscar(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item não encontrado"));
    }

    @Transactional
    public Item atualizar(UUID id, AtualizarItemRequest request) {
        Item item = buscar(id);
        Integer estoqueAtualizado = request.estoqueInformado() ? request.estoque() : item.getEstoque();
        validarEstoque(request.tipo(), estoqueAtualizado);
        validarNomeDisponivel(request.nome(), id);
        item.setNome(normalizarNome(request.nome()));
        item.setTipo(request.tipo());
        item.setPreco(request.preco());
        item.setEstoque(estoqueAtualizado);
        return salvar(item);
    }

    @Transactional
    public Item alterarAtivo(UUID id, boolean ativo) {
        Item item = buscar(id);
        item.setAtivo(ativo);
        return itemRepository.saveAndFlush(item);
    }

    @Transactional
    public void excluir(UUID id) {
        Item item = buscar(id);
        if (itemVendaRepository.existsByItemId(id)) {
            throw new ItemConflictException(
                    "Item possui histórico de vendas e não pode ser excluído. Inative o item para impedir novos usos."
            );
        }
        try {
            itemRepository.delete(item);
            itemRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ItemConflictException(
                    "Item possui vínculos e não pode ser excluído. Inative o item para impedir novos usos."
            );
        }
    }

    private void validarEstoque(TipoItem tipo, Integer estoque) {
        if (tipo == TipoItem.SERVICO && estoque != null) {
            throw new ItemValidationException("Serviço não pode possuir estoque");
        }
        if (tipo == TipoItem.PRODUTO && (estoque == null || estoque < 0)) {
            throw new ItemValidationException("Produto deve possuir estoque maior ou igual a zero");
        }
    }

    private void validarNomeDisponivel(String nome, UUID idAtual) {
        itemRepository.findByNomeIgnoreCase(nome.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(idAtual)) {
                throw new ItemConflictException("Já existe um item com esse nome");
            }
        });
    }

    private Item salvar(Item item) {
        try {
            return itemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException exception) {
            throw new ItemConflictException("Já existe um item com esse nome ou os dados violam uma regra do catálogo");
        }
    }

    private String normalizarNome(String nome) {
        return nome.trim();
    }

    private void validarPaginacao(int page, int size) {
        if (page < 0) {
            throw new ItemValidationException("page deve ser maior ou igual a zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ItemValidationException("size deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
    }
}
