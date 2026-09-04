package br.com.barbeirofinanceiro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "backup")
public class BackupProperties {

    private Path diretorio = Path.of("backups");
    private long tamanhoMaximoBytes = 536_870_912L;
    private String pgDump = "pg_dump";
    private String pgRestore = "pg_restore";
    private long timeoutSegundos = 300;

    public Path getDiretorio() { return diretorio; }
    public void setDiretorio(Path diretorio) {
        if (diretorio == null || diretorio.toString().isBlank()) {
            throw new IllegalArgumentException("backup.diretorio é obrigatório");
        }
        this.diretorio = diretorio;
    }
    public long getTamanhoMaximoBytes() { return tamanhoMaximoBytes; }
    public void setTamanhoMaximoBytes(long tamanhoMaximoBytes) {
        if (tamanhoMaximoBytes <= 0) {
            throw new IllegalArgumentException("backup.tamanho-maximo-bytes deve ser maior que zero");
        }
        this.tamanhoMaximoBytes = tamanhoMaximoBytes;
    }
    public String getPgDump() { return pgDump; }
    public void setPgDump(String pgDump) {
        this.pgDump = validarComando(pgDump, "backup.pg-dump");
    }
    public String getPgRestore() { return pgRestore; }
    public void setPgRestore(String pgRestore) {
        this.pgRestore = validarComando(pgRestore, "backup.pg-restore");
    }
    public long getTimeoutSegundos() { return timeoutSegundos; }
    public void setTimeoutSegundos(long timeoutSegundos) {
        if (timeoutSegundos <= 0) {
            throw new IllegalArgumentException("backup.timeout-segundos deve ser maior que zero");
        }
        this.timeoutSegundos = timeoutSegundos;
    }

    private String validarComando(String comando, String propriedade) {
        if (comando == null || comando.isBlank()) {
            throw new IllegalArgumentException(propriedade + " é obrigatório");
        }
        return comando;
    }
}
