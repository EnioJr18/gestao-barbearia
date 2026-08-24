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
                .body(CaixaResponse.from(caixa));
    }

    @GetMapping("/atual")
    public CaixaResponse atual() {
        return CaixaResponse.from(caixaService.atual());
    }

    @PostMapping("/{id}/fechar")
    public CaixaResponse fechar(
            @PathVariable UUID id,
            @Valid @RequestBody FecharCaixaRequest request,
            Authentication authentication
    ) {
        return CaixaResponse.from(caixaService.fechar(id, request.valorApurado(), authentication));
    }
}
