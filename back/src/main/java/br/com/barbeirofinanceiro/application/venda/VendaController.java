package br.com.barbeirofinanceiro.application.venda;
import br.com.barbeirofinanceiro.domain.venda.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
@RestController
@RequestMapping("/api/v1/vendas")
public class VendaController {
    private final VendaService service;
    public VendaController(VendaService service){this.service=service;}
    @PostMapping public ResponseEntity<VendaResponse> criar(@Valid @RequestBody CriarVendaRequest r){Venda v=service.criar(r);return ResponseEntity.created(URI.create("/api/v1/vendas/"+v.getId())).body(res(v));}
    @GetMapping("/{id}") public VendaResponse buscar(@PathVariable UUID id){return res(service.carregar(id));}
    @GetMapping public List<VendaResponse> listar(@RequestParam(required=false) LocalDate dataInicial,@RequestParam(required=false) LocalDate dataFinal,@RequestParam(required=false) UUID cliente,@RequestParam(required=false) StatusVenda status){return service.listar(dataInicial,dataFinal,cliente,status).stream().map(this::res).toList();}
    @PostMapping("/{id}/cancelar") public VendaResponse cancelar(@PathVariable UUID id){return res(service.cancelar(id));}
    private VendaResponse res(Venda v){return VendaResponse.from(v,service.itens(v.getId()),service.pagamentos(v.getId()));}
}
