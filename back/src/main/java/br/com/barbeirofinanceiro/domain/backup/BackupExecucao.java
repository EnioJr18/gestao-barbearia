package br.com.barbeirofinanceiro.domain.backup;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backup_execucoes")
public class BackupExecucao {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupTipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupStatus status;

    @Column(length = 500)
    private String arquivo;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "inicio_em", nullable = false)
    private Instant inicioEm;

    @Column(name = "fim_em")
    private Instant fimEm;

    @Column(columnDefinition = "TEXT")
    private String erro;

    @PrePersist void prePersist() { if (inicioEm == null) inicioEm = Instant.now(); }
    protected BackupExecucao() {}

    public static BackupExecucao iniciar(BackupTipo tipo, String arquivo) {
        BackupExecucao execucao = new BackupExecucao();
        execucao.setTipo(tipo);
        execucao.setStatus(BackupStatus.INICIADO);
        execucao.setArquivo(arquivo);
        execucao.setInicioEm(Instant.now());
        return execucao;
    }

    public UUID getId() { return id; }
    public BackupTipo getTipo() { return tipo; }
    public void setTipo(BackupTipo tipo) { this.tipo = tipo; }
    public BackupStatus getStatus() { return status; }
    public void setStatus(BackupStatus status) { this.status = status; }
    public String getArquivo() { return arquivo; }
    public void setArquivo(String arquivo) { this.arquivo = arquivo; }
    public Long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public Instant getInicioEm() { return inicioEm; }
    public void setInicioEm(Instant inicioEm) { this.inicioEm = inicioEm; }
    public Instant getFimEm() { return fimEm; }
    public void setFimEm(Instant fimEm) { this.fimEm = fimEm; }
    public String getErro() { return erro; }
    public void setErro(String erro) { this.erro = erro; }
}
