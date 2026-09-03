package br.com.barbeirofinanceiro.application.backup;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class BackupExceptionHandler {

    @ExceptionHandler(BackupValidationException.class)
    ResponseEntity<Map<String, String>> validation(BackupValidationException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(BackupNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(BackupNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BackupConflictException.class)
    ResponseEntity<Map<String, String>> conflict(BackupConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(BackupExecutionException.class)
    ResponseEntity<Map<String, String>> execution(BackupExecutionException exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
