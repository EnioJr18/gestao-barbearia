package br.com.barbeirofinanceiro.application.despesa;

import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import br.com.barbeirofinanceiro.domain.movimentacao.Movimentacao;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class DespesaController {

    private final MovimentacaoDespesaService movimentacaoDespesaService;
    private final DespesaRecorrenteService despesaRecorrenteService;

    public DespesaController(
            MovimentacaoDespesaService movimentacaoDespesaService,
            DespesaRecorrenteService despesaRecorrenteService
    ) {
        this.movimentacaoDespesaService = movimentacaoDespesaService;
        this.despesaRecorrenteService = despesaRecorrenteService;
    }

    @PostMapping("/api/v1/movimentacoes")
    public ResponseEntity<DespesaResponse> criar(@Valid @RequestBody CriarMovimentacaoRequest request) {
        Movimentacao movimentacao = movimentacaoDespesaService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/movimentacoes/" + movimentacao.getId()))
                .body(DespesaResponse.from(movimentacao));
    }

    @GetMapping("/api/v1/movimentacoes")
    public List<DespesaResponse> listar(
            @RequestParam(required = false) LocalDate dataInicial,
            @RequestParam(required = false) LocalDate dataFinal,
            @RequestParam(required = false) UUID categoria,
            @RequestParam(required = false) FormaPagamento formaPagamento
    ) {
        return movimentacaoDespesaService.listar(dataInicial, dataFinal, categoria, formaPagamento)
                .stream()
                .map(DespesaResponse::from)
                .toList();
    }

    @GetMapping("/api/v1/movimentacoes/{id}")
    public DespesaResponse buscar(@PathVariable UUID id) {
        return DespesaResponse.from(movimentacaoDespesaService.buscar(id));
    }

    @PostMapping("/api/v1/despesas-recorrentes")
    public ResponseEntity<DespesaRecorrenteResponse> criarRecorrente(
            @Valid @RequestBody CriarDespesaRecorrenteRequest request
    ) {
        var despesaRecorrente = despesaRecorrenteService.criar(request);
        return ResponseEntity.created(
                        URI.create("/api/v1/despesas-recorrentes/" + despesaRecorrente.getId())
                )
                .body(DespesaRecorrenteResponse.from(despesaRecorrente));
    }

    @GetMapping("/api/v1/despesas-recorrentes")
    public List<DespesaRecorrenteResponse> listarRecorrentes() {
        return despesaRecorrenteService.listar().stream()
                .map(DespesaRecorrenteResponse::from)
                .toList();
    }

    @GetMapping("/api/v1/despesas-recorrentes/{id}")
    public DespesaRecorrenteResponse buscarRecorrente(@PathVariable UUID id) {
        return DespesaRecorrenteResponse.from(despesaRecorrenteService.buscar(id));
    }

    @PutMapping("/api/v1/despesas-recorrentes/{id}")
    public DespesaRecorrenteResponse atualizarRecorrente(
            @PathVariable UUID id,
            @Valid @RequestBody CriarDespesaRecorrenteRequest request
    ) {
        return DespesaRecorrenteResponse.from(despesaRecorrenteService.atualizar(id, request));
    }

    @PatchMapping("/api/v1/despesas-recorrentes/{id}/ativar")
    public DespesaRecorrenteResponse ativarRecorrente(@PathVariable UUID id) {
        return DespesaRecorrenteResponse.from(despesaRecorrenteService.alterarAtiva(id, true));
    }

    @PatchMapping("/api/v1/despesas-recorrentes/{id}/inativar")
    public DespesaRecorrenteResponse inativarRecorrente(@PathVariable UUID id) {
        return DespesaRecorrenteResponse.from(despesaRecorrenteService.alterarAtiva(id, false));
    }
}
