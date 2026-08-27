package br.com.barbeirofinanceiro.application.cliente;

import br.com.barbeirofinanceiro.domain.cliente.Cliente;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) { this.clienteService = clienteService; }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody CriarClienteRequest request) {
        Cliente cliente = clienteService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/clientes/" + cliente.getId()))
                .body(ClienteResponse.from(cliente));
    }

    @GetMapping
    public List<ClienteResponse> listar(@RequestParam(required = false) String nome,
                                        @RequestParam(required = false) Boolean ativo) {
        return clienteService.listar(nome, ativo).stream().map(ClienteResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ClienteResponse buscar(@PathVariable UUID id) {
        return ClienteResponse.from(clienteService.buscar(id));
    }

    @PutMapping("/{id}")
    public ClienteResponse atualizar(@PathVariable UUID id,
                                     @Valid @RequestBody AtualizarClienteRequest request) {
        return ClienteResponse.from(clienteService.atualizar(id, request));
    }

    @PatchMapping("/{id}/ativar")
    public ClienteResponse ativar(@PathVariable UUID id) {
        return ClienteResponse.from(clienteService.alterarAtivo(id, true));
    }

    @PatchMapping("/{id}/inativar")
    public ClienteResponse inativar(@PathVariable UUID id) {
        return ClienteResponse.from(clienteService.alterarAtivo(id, false));
    }
}
