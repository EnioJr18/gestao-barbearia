package br.com.barbeirofinanceiro.application.caixa;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class CaixaExceptionHandler {
    @ExceptionHandler(CaixaValidationException.class)
    ResponseEntity<Map<String, String>> validation(CaixaValidationException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> beanValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Payload inválido");
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(CaixaNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(CaixaNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CaixaConflictException.class)
    ResponseEntity<Map<String, String>> conflict(CaixaConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(AuthenticatedUserException.class)
    ResponseEntity<Map<String, String>> unauthorized(AuthenticatedUserException exception) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
