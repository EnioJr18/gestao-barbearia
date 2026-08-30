package br.com.barbeirofinanceiro.application.relatorio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public RelatorioResumoResponse buscarResumo(
            @RequestParam LocalDate dataInicial,
            @RequestParam LocalDate dataFinal
    ) {
        return service.buscarResumo(dataInicial, dataFinal);
    }

    @GetMapping("/pagamentos")
    public RelatorioPagamentoResponse buscarPagamentos(
            @RequestParam LocalDate dataInicial,
            @RequestParam LocalDate dataFinal
    ) {
        return service.buscarPagamentos(dataInicial, dataFinal);
    }

    @GetMapping("/itens")
    public List<ItemRelatorioResponse> buscarItens(
            @RequestParam LocalDate dataInicial,
            @RequestParam LocalDate dataFinal
    ) {
        return service.buscarItens(dataInicial, dataFinal);
    }

    @GetMapping("/clientes")
    public List<ClienteRelatorioResponse> buscarClientes(
            @RequestParam LocalDate dataInicial,
            @RequestParam LocalDate dataFinal
    ) {
        return service.buscarClientes(dataInicial, dataFinal);
    }
}
