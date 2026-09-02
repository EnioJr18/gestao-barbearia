package br.com.barbeirofinanceiro.application.auth;

public record LoginResponse(
        String token,
        String tipo
) {
}