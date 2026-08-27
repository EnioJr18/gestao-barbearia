package br.com.barbeirofinanceiro.application.item;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ItemExceptionHandler {
    @ExceptionHandler(ItemValidationException.class)
    ResponseEntity<Map<String, String>> validation(ItemValidationException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ItemNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(ItemNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ItemConflictException.class)
    ResponseEntity<Map<String, String>> conflict(ItemConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
