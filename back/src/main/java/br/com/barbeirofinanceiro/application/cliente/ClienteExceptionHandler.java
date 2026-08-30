package br.com.barbeirofinanceiro.application.cliente;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ClienteExceptionHandler {
    @ExceptionHandler(ClienteNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(ClienteNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }
}
