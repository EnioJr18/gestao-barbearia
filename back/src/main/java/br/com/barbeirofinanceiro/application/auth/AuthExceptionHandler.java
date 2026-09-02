package br.com.barbeirofinanceiro.application.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleBadCredentials(
            BadCredentialsException exception
    ) {
        return Map.of(
                "message",
                "E-mail ou senha inválidos"
        );
    }
}