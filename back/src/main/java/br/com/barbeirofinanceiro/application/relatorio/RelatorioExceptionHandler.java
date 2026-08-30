package br.com.barbeirofinanceiro.application.relatorio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class RelatorioExceptionHandler {

    @ExceptionHandler(RelatorioValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(RelatorioValidationException exception) {
        return Map.of("message", exception.getMessage());
    }
}