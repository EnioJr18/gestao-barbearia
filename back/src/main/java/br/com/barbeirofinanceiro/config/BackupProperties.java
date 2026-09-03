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
    public void setDiretorio(Path diretorio) { this.diretorio = diretorio; }
    public long getTamanhoMaximoBytes() { return tamanhoMaximoBytes; }
    public void setTamanhoMaximoBytes(long tamanhoMaximoBytes) { this.tamanhoMaximoBytes = tamanhoMaximoBytes; }
    public String getPgDump() { return pgDump; }
    public void setPgDump(String pgDump) { this.pgDump = pgDump; }
    public String getPgRestore() { return pgRestore; }
    public void setPgRestore(String pgRestore) { this.pgRestore = pgRestore; }
    public long getTimeoutSegundos() { return timeoutSegundos; }
    public void setTimeoutSegundos(long timeoutSegundos) { this.timeoutSegundos = timeoutSegundos; }
}
