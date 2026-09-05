package br.com.barbeirofinanceiro.application.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse buscar(
            @RequestParam(required = false) LocalDate dataInicial,
            @RequestParam(required = false) LocalDate dataFinal
    ) {
        return service.buscar(dataInicial, dataFinal);
    }
}
