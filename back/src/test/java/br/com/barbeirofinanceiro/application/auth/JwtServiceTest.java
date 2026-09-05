package br.com.barbeirofinanceiro.application.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtProperties properties = new JwtProperties(
            "uma-chave-de-desenvolvimento-bem-grande...",
            3600000L
    );

    private final JwtService service = new JwtService(properties);

    @Test
    void deveGerarToken() {
        UserDetails user = User.withUsername("usuario@teste.local")
                .password("hash")
                .roles("USER")
                .build();

        String token = service.gerarToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void deveExtrairEmailDoToken() {
        UserDetails user = User.withUsername("usuario@teste.local")
                .password("hash")
                .roles("USER")
                .build();

        String token = service.gerarToken(user);

        assertEquals("usuario@teste.local", service.extrairUsername(token));
    }

    @Test
    void deveValidarToken() {
        UserDetails user = User.withUsername("usuario@teste.local")
                .password("hash")
                .roles("USER")
                .build();

        String token = service.gerarToken(user);

        assertTrue(service.validarToken(token, user));
    }

    @Test
    void deveValidarTokenNaoPertenceAoOutroUsuario() {
        UserDetails user = User.withUsername("usuario@teste.local")
                .password("hash")
                .roles("USER")
                .build();

        UserDetails outroUsuario = User.withUsername("outro@teste.local")
                .password("hash")
                .roles("USER")
                .build();

        String token = service.gerarToken(user);

        assertFalse(service.validarToken(token, outroUsuario));
    }

    @Test
    void deveInvalidarTokenQuandoHashDaSenhaMudar() {
        UserDetails usuarioAntesDaTroca = User.withUsername("usuario@teste.local")
                .password("hash-antigo")
                .roles("USER")
                .build();
        UserDetails usuarioDepoisDaTroca = User.withUsername("usuario@teste.local")
                .password("hash-novo")
                .roles("USER")
                .build();

        String token = service.gerarToken(usuarioAntesDaTroca);

        assertFalse(service.validarToken(token, usuarioDepoisDaTroca));
    }

    @Test
    void deveRejeitarTokenLegadoSemVersaoDeCredencial() {
        UserDetails user = User.withUsername("usuario@teste.local")
                .password("hash")
                .roles("USER")
                .build();
        String tokenLegado = Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + properties.expiration()))
                .signWith(Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertFalse(service.validarToken(tokenLegado, user));
    }

    @Test
    void deveValidarTokenInvalido() {
        UserDetails user = User.withUsername("usuario@teste.local")
                .password("hash")
                .roles("USER")
                .build();

        String tokenInvalido = "token-invalido";

        assertThrows(RuntimeException.class, () -> service.extrairUsername(tokenInvalido));
        assertFalse(service.validarToken(tokenInvalido, user));
    }
}
