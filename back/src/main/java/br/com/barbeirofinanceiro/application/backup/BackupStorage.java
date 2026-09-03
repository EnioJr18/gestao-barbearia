package br.com.barbeirofinanceiro.application.backup;

import br.com.barbeirofinanceiro.config.BackupProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class BackupStorage {

    private static final String EXTENSAO = ".backup";
    private static final DateTimeFormatter NOME_DATA = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private final BackupProperties properties;

    public BackupStorage(BackupProperties properties) {
        this.properties = properties;
    }

    public Path criarTemporario() {
        try {
            return Files.createTempFile(diretorioBase(), ".backup-", ".tmp");
        } catch (IOException exception) {
            throw new BackupExecutionException("Não foi possível preparar o arquivo de backup", exception);
        }
    }

    public String gerarNomeArquivo() {
        return "backup-" + NOME_DATA.format(Instant.now()) + "-" + UUID.randomUUID() + EXTENSAO;
    }

    public Path publicar(Path temporario, String arquivo) {
        Path destino = resolverNovo(arquivo);
        try {
            try {
                return Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                return Files.move(temporario, destino);
            }
        } catch (IOException exception) {
            throw new BackupExecutionException("Não foi possível publicar o arquivo de backup", exception);
        }
    }

    public Path resolverExistente(String arquivo) {
        validarNome(arquivo);
        Path caminho = diretorioBase().resolve(arquivo).normalize();
        validarDentroDoDiretorio(caminho);
        if (!Files.exists(caminho, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(caminho, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(caminho)) {
            throw new BackupNotFoundException("Arquivo de backup não encontrado");
        }
        validarTamanho(caminho);
        return caminho;
    }

    public List<Path> listar() {
        try (var arquivos = Files.list(diretorioBase())) {
            return arquivos
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(path -> path.getFileName().toString().endsWith(EXTENSAO))
                    .sorted(Comparator.comparing(this::ultimaModificacao).reversed())
                    .toList();
        } catch (IOException exception) {
            throw new BackupExecutionException("Não foi possível listar os arquivos de backup", exception);
        }
    }

    public long tamanho(Path caminho) {
        try {
            return Files.size(caminho);
        } catch (IOException exception) {
            throw new BackupExecutionException("Não foi possível obter o tamanho do backup", exception);
        }
    }

    public void validarTamanhoArquivo(Path caminho) {
        validarTamanho(caminho);
    }

    public Instant ultimaModificacao(Path caminho) {
        try {
            return Files.getLastModifiedTime(caminho, LinkOption.NOFOLLOW_LINKS).toInstant();
        } catch (IOException exception) {
            throw new BackupExecutionException("Não foi possível obter os metadados do backup", exception);
        }
    }

    public void excluirSilenciosamente(Path caminho) {
        if (caminho == null) {
            return;
        }
        try {
            Files.deleteIfExists(caminho);
        } catch (IOException ignored) {
            // O erro original da operação de backup permanece como diagnóstico principal.
        }
    }

    public Path diretorioBase() {
        try {
            Path base = properties.getDiretorio().toAbsolutePath().normalize();
            Files.createDirectories(base);
            return base;
        } catch (IOException exception) {
            throw new BackupExecutionException("Não foi possível preparar o diretório de backups", exception);
        }
    }

    private Path resolverNovo(String arquivo) {
        validarNome(arquivo);
        Path caminho = diretorioBase().resolve(arquivo).normalize();
        validarDentroDoDiretorio(caminho);
        if (Files.exists(caminho, LinkOption.NOFOLLOW_LINKS)) {
            throw new BackupConflictException("Já existe um arquivo de backup com esse nome");
        }
        return caminho;
    }

    private void validarNome(String arquivo) {
        if (arquivo == null || !arquivo.matches("backup-[0-9]{8}T[0-9]{6}Z-[0-9a-fA-F-]{36}\\.backup")) {
            throw new BackupValidationException("Identificador de backup inválido");
        }
        Path nome = Path.of(arquivo);
        if (nome.isAbsolute() || nome.getNameCount() != 1 || !nome.getFileName().toString().equals(arquivo)) {
            throw new BackupValidationException("Identificador de backup inválido");
        }
    }

    private void validarDentroDoDiretorio(Path caminho) {
        if (!caminho.startsWith(diretorioBase())) {
            throw new BackupValidationException("Identificador de backup inválido");
        }
    }

    private void validarTamanho(Path caminho) {
        long tamanho = tamanho(caminho);
        if (tamanho <= 0 || tamanho > properties.getTamanhoMaximoBytes()) {
            throw new BackupValidationException("Arquivo de backup inválido");
        }
    }
}
