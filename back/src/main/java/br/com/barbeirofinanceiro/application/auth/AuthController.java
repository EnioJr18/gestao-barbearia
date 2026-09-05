package br.com.barbeirofinanceiro.application.auth;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService service;

    public AuthController(AuthenticationService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return service.autenticar(request);
    }
}