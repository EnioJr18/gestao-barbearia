package br.com.barbeirofinanceiro.application.backup;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Component
public class RestauracaoMetadataStore {

    private final BackupStorage storage;
    public RestauracaoMetadataStore(BackupStorage storage) {
        this.storage = storage;
    }

    public void registrar(String arquivo, String status, String erro) {
        Path diretorio = storage.diretorioBase().resolve("operacoes");
        try {
            java.nio.file.Files.createDirectories(diretorio);
            Path destino = diretorio.resolve("restauracao-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".json");
            String conteudo = "{\"arquivo\":\"" + escapar(arquivo)
                    + "\",\"status\":\"" + escapar(status)
                    + "\",\"executadoEm\":\"" + Instant.now()
                    + "\",\"erro\":\"" + escapar(erro == null ? "" : erro) + "\"}";
            java.nio.file.Files.writeString(destino, conteudo);
        } catch (IOException exception) {
            throw new BackupExecutionException("Não foi possível registrar os metadados da restauração", exception);
        }
    }

    private String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
