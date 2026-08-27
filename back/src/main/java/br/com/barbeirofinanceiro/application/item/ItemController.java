package br.com.barbeirofinanceiro.application.item;

import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/itens")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> criar(@Valid @RequestBody CriarItemRequest request) {
        Item item = itemService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/itens/" + item.getId()))
                .body(ItemResponse.from(item));
    }

    @GetMapping
    public List<ItemResponse> listar(
            @RequestParam(required = false) TipoItem tipo,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String nome
    ) {
        return itemService.listar(tipo, ativo, nome).stream().map(ItemResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ItemResponse buscar(@PathVariable UUID id) {
        return ItemResponse.from(itemService.buscar(id));
    }

    @PutMapping("/{id}")
    public ItemResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarItemRequest request) {
        return ItemResponse.from(itemService.atualizar(id, request));
    }

    @PatchMapping("/{id}/ativar")
    public ItemResponse ativar(@PathVariable UUID id) {
        return ItemResponse.from(itemService.alterarAtivo(id, true));
    }

    @PatchMapping("/{id}/inativar")
    public ItemResponse inativar(@PathVariable UUID id) {
        return ItemResponse.from(itemService.alterarAtivo(id, false));
    }
}
