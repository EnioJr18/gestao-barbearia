package br.com.barbeirofinanceiro.application.usuario;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public UsuarioResponse consultarAtual(Authentication authentication) {
        return usuarioService.consultarAtual(authentication);
    }

    @PutMapping("/me")
    public UsuarioResponse atualizarAtual(
            Authentication authentication,
            @Valid @RequestBody AtualizarUsuarioRequest request
    ) {
        return usuarioService.atualizarAtual(authentication, request);
    }

    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarSenhaAtual(
            Authentication authentication,
            @Valid @RequestBody AlterarSenhaRequest request
    ) {
        usuarioService.alterarSenhaAtual(authentication, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/desativar")
    public ResponseEntity<Void> desativarAtual(Authentication authentication) {
        usuarioService.desativarAtual(authentication);
        return ResponseEntity.noContent().build();
    }
}
