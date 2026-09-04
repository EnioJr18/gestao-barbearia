package br.com.barbeirofinanceiro.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackupPropertiesTest {

    @Test
    void deveRejeitarDiretorioAusente() {
        BackupProperties properties = new BackupProperties();

        assertThatThrownBy(() -> properties.setDiretorio(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("backup.diretorio é obrigatório");
    }

    @Test
    void deveRejeitarTamanhoETimeoutNaoPositivos() {
        BackupProperties properties = new BackupProperties();

        assertThatThrownBy(() -> properties.setTamanhoMaximoBytes(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("backup.tamanho-maximo-bytes deve ser maior que zero");
        assertThatThrownBy(() -> properties.setTimeoutSegundos(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("backup.timeout-segundos deve ser maior que zero");
    }

    @Test
    void deveAceitarDiretorioValido() {
        BackupProperties properties = new BackupProperties();

        properties.setDiretorio(Path.of("backups"));

        org.assertj.core.api.Assertions.assertThat(properties.getDiretorio()).isEqualTo(Path.of("backups"));
    }
}
