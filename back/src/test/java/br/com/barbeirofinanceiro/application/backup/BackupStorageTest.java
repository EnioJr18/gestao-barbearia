package br.com.barbeirofinanceiro.application.backup;

import br.com.barbeirofinanceiro.config.BackupProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupStorageTest {

    @TempDir
    Path diretorioTemporario;

    @Test
    void deveResolverBackupValidoDentroDoDiretorioControlado() throws Exception {
        BackupStorage storage = storage(1024);
        String nome = storage.gerarNomeArquivo();
        Path arquivo = diretorioTemporario.resolve(nome);
        Files.writeString(arquivo, "conteudo");

        assertEquals(arquivo.toAbsolutePath(), storage.resolverExistente(nome));
    }

    @Test
    void deveRejeitarPathTraversal() {
        BackupStorage storage = storage(1024);

        assertThrows(BackupValidationException.class,
                () -> storage.resolverExistente("../backup-20260903T000000Z-123e4567-e89b-12d3-a456-426614174000.backup"));
    }

    @Test
    void deveRejeitarExtensaoInvalida() {
        BackupStorage storage = storage(1024);

        assertThrows(BackupValidationException.class,
                () -> storage.resolverExistente("backup-20260903T000000Z-123e4567-e89b-12d3-a456-426614174000.sql"));
    }

    @Test
    void deveRejeitarArquivoAcimaDoLimite() throws Exception {
        BackupStorage storage = storage(3);
        String nome = storage.gerarNomeArquivo();
        Files.writeString(diretorioTemporario.resolve(nome), "muito grande");

        assertThrows(BackupValidationException.class, () -> storage.resolverExistente(nome));
    }

    @Test
    void deveCriarEPublicarArquivoSemSobrescrever() throws Exception {
        BackupStorage storage = storage(1024);
        String nome = storage.gerarNomeArquivo();
        Path temporario = storage.criarTemporario();
        Files.writeString(temporario, "conteudo");

        Path publicado = storage.publicar(temporario, nome);

        assertTrue(Files.exists(publicado));
        assertFalse(Files.exists(temporario));
        assertThrows(BackupConflictException.class, () -> storage.publicar(storage.criarTemporario(), nome));
    }

    private BackupStorage storage(long limite) {
        BackupProperties properties = new BackupProperties();
        properties.setDiretorio(diretorioTemporario);
        properties.setTamanhoMaximoBytes(limite);
        return new BackupStorage(properties);
    }
}
