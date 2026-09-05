package br.com.barbeirofinanceiro.application.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final String CREDENTIAL_VERSION_CLAIM = "cv";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(
                properties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String gerarToken(UserDetails userDetails) {
        Date agora = new Date();
        Date expiracao = new Date(
                agora.getTime() + properties.expiration()
        );

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(CREDENTIAL_VERSION_CLAIM, versaoCredencial(userDetails.getPassword()))
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(key)
                .compact();
    }

    public String extrairUsername(String token) {
        return extrairClaims(token).getSubject();
    }

    private io.jsonwebtoken.Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validarToken(
            String token,
            UserDetails userDetails
    ) {
        try {
            var claims = extrairClaims(token);
            String username = claims.getSubject();
            String versaoDoToken = claims.get(CREDENTIAL_VERSION_CLAIM, String.class);

            return username.equals(userDetails.getUsername())
                    && versaoDoToken != null
                    && versaoDoToken.equals(versaoCredencial(userDetails.getPassword()));

        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String versaoCredencial(String senhaHash) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(senhaHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível", exception);
        }
    }
}
