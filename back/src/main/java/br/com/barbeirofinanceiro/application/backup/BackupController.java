package br.com.barbeirofinanceiro.application.backup;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/backups")
public class BackupController {

    private final BackupService service;

    public BackupController(BackupService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BackupResponse> criar() {
        BackupResponse response = service.criar();
        return ResponseEntity.created(URI.create("/api/v1/backups/" + response.arquivo())).body(response);
    }

    @GetMapping
    public List<BackupResponse> listar() {
        return service.listar();
    }

    @PostMapping("/{arquivo}/restaurar")
    public RestauracaoResponse restaurar(@PathVariable String arquivo) {
        return service.restaurar(arquivo);
    }
}
