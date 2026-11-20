package br.com.barbeirofinanceiro.application.venda;

import br.com.barbeirofinanceiro.application.common.PageResponse;
import br.com.barbeirofinanceiro.domain.venda.StatusVenda;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendas")
public class VendaController {

    private final VendaService service;

    public VendaController(VendaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<VendaResponse> criar(@Valid @RequestBody CriarVendaRequest request) {
        VendaResponse response = service.criarComResposta(request);
        return ResponseEntity.created(URI.create("/api/v1/vendas/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public VendaResponse buscar(@PathVariable UUID id) {
        return service.buscarComResposta(id);
    }

    @PutMapping("/{id}")
    public VendaResponse atualizar(@PathVariable UUID id,
                                   @Valid @RequestBody CriarVendaRequest request) {
        return service.atualizarComResposta(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public PageResponse<VendaResponse> listar(
            @RequestParam(required = false) LocalDate dataInicial,
            @RequestParam(required = false) LocalDate dataFinal,
            @RequestParam(required = false) UUID cliente,
            @RequestParam(required = false) StatusVenda status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.listarRespostas(dataInicial, dataFinal, cliente, status, page, size);
    }

    @PostMapping("/{id}/cancelar")
    public VendaResponse cancelar(@PathVariable UUID id) {
        return service.cancelarComResposta(id);
    }
}
