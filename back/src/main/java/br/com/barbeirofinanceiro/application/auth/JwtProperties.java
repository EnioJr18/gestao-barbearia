package br.com.barbeirofinanceiro.application.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long expiration
) {
    private static final int HMAC_SHA256_MINIMUM_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret é obrigatório");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < HMAC_SHA256_MINIMUM_BYTES) {
            throw new IllegalArgumentException("jwt.secret deve possuir ao menos 32 bytes");
        }
        if (expiration <= 0) {
            throw new IllegalArgumentException("jwt.expiration deve ser maior que zero");
        }
    }
}
