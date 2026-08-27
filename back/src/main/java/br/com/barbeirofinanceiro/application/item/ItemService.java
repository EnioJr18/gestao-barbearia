package br.com.barbeirofinanceiro.application.item;

import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.ItemRepository;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public Item criar(CriarItemRequest request) {
        validarEstoque(request.tipo(), request.estoque());
        validarNomeDisponivel(request.nome(), null);

        Item item = new Item();
        item.setNome(normalizarNome(request.nome()));
        item.setTipo(request.tipo());
        item.setPreco(request.preco());
        item.setEstoque(request.estoque());
        return salvar(item);
    }

    @Transactional(readOnly = true)
    public List<Item> listar(TipoItem tipo, Boolean ativo, String nome) {
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
        return itemRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "nome"));
    }

    @Transactional(readOnly = true)
    public Item buscar(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item não encontrado"));
    }

    @Transactional
    public Item atualizar(UUID id, AtualizarItemRequest request) {
        Item item = buscar(id);
        if (request.tipo() == TipoItem.SERVICO && item.getEstoque() != null) {
            throw new ItemConflictException("Não é possível transformar produto com estoque em serviço");
        }
        if (request.tipo() == TipoItem.PRODUTO && item.getEstoque() == null) {
            throw new ItemValidationException("Produto deve possuir estoque");
        }
        validarNomeDisponivel(request.nome(), id);
        item.setNome(normalizarNome(request.nome()));
        item.setTipo(request.tipo());
        item.setPreco(request.preco());
        return salvar(item);
    }

    @Transactional
    public Item alterarAtivo(UUID id, boolean ativo) {
        Item item = buscar(id);
        item.setAtivo(ativo);
        return itemRepository.saveAndFlush(item);
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
}
