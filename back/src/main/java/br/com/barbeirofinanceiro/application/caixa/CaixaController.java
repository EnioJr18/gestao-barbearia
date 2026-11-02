package br.com.barbeirofinanceiro.application.caixa;

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
        CaixaResponse response = caixaService.abrirComResposta(
                request.valorInicial(),
                authentication
        );
        return ResponseEntity.created(URI.create("/api/v1/caixas/" + response.id()))
                .body(response);
    }

    @GetMapping("/atual")
    public ResponseEntity<CaixaResponse> atual() {
        return caixaService.atualComRespostaOpcional()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.<CaixaResponse>noContent().build());
    }

    @PostMapping("/{id}/fechar")
    public CaixaResponse fechar(
            @PathVariable UUID id,
            @Valid @RequestBody FecharCaixaRequest request,
            Authentication authentication
    ) {
        return caixaService.fecharComResposta(id, request.valorApurado(), authentication);
    }
}
