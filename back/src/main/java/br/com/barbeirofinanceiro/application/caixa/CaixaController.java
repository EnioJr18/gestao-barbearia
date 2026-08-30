package br.com.barbeirofinanceiro.application.caixa;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/caixas")
public class CaixaController {
    private final CaixaService caixaService;

    public CaixaController(CaixaService caixaService) {
        this.caixaService = caixaService;
    }

    @PostMapping("/abrir")
    public ResponseEntity<CaixaResponse> abrir(
            @Valid @RequestBody AbrirCaixaRequest request,
            Authentication authentication
    ) {
        Caixa caixa = caixaService.abrir(request.valorInicial(), authentication);
        return ResponseEntity.created(URI.create("/api/v1/caixas/" + caixa.getId()))
                .body(resposta(caixa));
    }

    @GetMapping("/atual")
    public CaixaResponse atual() {
        return resposta(caixaService.atual());
    }

    @PostMapping("/{id}/fechar")
    public CaixaResponse fechar(
            @PathVariable UUID id,
            @Valid @RequestBody FecharCaixaRequest request,
            Authentication authentication
    ) {
        Caixa caixa = caixaService.fechar(id, request.valorApurado(), authentication);
        return resposta(caixa);
    }

    private CaixaResponse resposta(Caixa caixa) {
        var entradas = caixaService.entradasDinheiro(caixa);
        var saidas = caixaService.saidasDinheiro(caixa);
        return CaixaResponse.from(caixa, entradas, saidas, caixa.getStatus() == br.com.barbeirofinanceiro.domain.caixa.StatusCaixa.FECHADO
                ? caixa.getValorApurado().subtract(caixa.getDiferenca()) : caixaService.valorEsperado(caixa));
    }
}
