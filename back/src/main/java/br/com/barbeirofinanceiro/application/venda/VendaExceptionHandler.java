package br.com.barbeirofinanceiro.application.venda;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestControllerAdvice
public class VendaExceptionHandler {
    @ExceptionHandler(VendaValidationException.class)
    ResponseEntity<Map<String,String>> validation(VendaValidationException e) { return response(HttpStatus.BAD_REQUEST,e.getMessage()); }
    @ExceptionHandler(VendaNotFoundException.class)
    ResponseEntity<Map<String,String>> notFound(VendaNotFoundException e) { return response(HttpStatus.NOT_FOUND,e.getMessage()); }
    @ExceptionHandler(VendaConflictException.class)
    ResponseEntity<Map<String,String>> conflict(VendaConflictException e) { return response(HttpStatus.CONFLICT,e.getMessage()); }
    private ResponseEntity<Map<String,String>> response(HttpStatus s,String m) { return ResponseEntity.status(s).body(Map.of("message",m)); }
}
