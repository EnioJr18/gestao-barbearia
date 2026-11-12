package br.com.barbeirofinanceiro.application.cliente;

import br.com.barbeirofinanceiro.domain.cliente.Cliente;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) { this.clienteService = clienteService; }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody CriarClienteRequest request) {
        Cliente cliente = clienteService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/clientes/" + cliente.getId()))
                .body(ClienteResponse.from(cliente, clienteService.resumo(cliente.getId())));
    }

    @GetMapping
    public List<ClienteResponse> listar(@RequestParam(required = false) String nome,
                                        @RequestParam(required = false) Boolean ativo) {
        Map<UUID, ClienteResumoProjection> resumos = clienteService.resumos().stream()
                .collect(Collectors.toMap(ClienteResumoProjection::clienteId, Function.identity()));
        return clienteService.listar(nome, ativo).stream()
                .map(cliente -> ClienteResponse.from(cliente, resumos.get(cliente.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public ClienteResponse buscar(@PathVariable UUID id) {
        return ClienteResponse.from(clienteService.buscar(id), clienteService.resumo(id));
    }

    @PutMapping("/{id}")
    public ClienteResponse atualizar(@PathVariable UUID id,
                                     @Valid @RequestBody AtualizarClienteRequest request) {
        return ClienteResponse.from(clienteService.atualizar(id, request), clienteService.resumo(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ClienteResponse ativar(@PathVariable UUID id) {
        return ClienteResponse.from(clienteService.alterarAtivo(id, true), clienteService.resumo(id));
    }

    @PatchMapping("/{id}/inativar")
    public ClienteResponse inativar(@PathVariable UUID id) {
        return ClienteResponse.from(clienteService.alterarAtivo(id, false), clienteService.resumo(id));
    }
}
