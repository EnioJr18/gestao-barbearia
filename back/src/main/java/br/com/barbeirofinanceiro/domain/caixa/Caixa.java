package br.com.barbeirofinanceiro.domain.caixa;

import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "caixas")
public class Caixa {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "data_caixa", nullable = false)
    private LocalDate dataCaixa;

    @Column(name = "valor_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorInicial;

    @Column(name = "valor_apurado", precision = 12, scale = 2)
    private BigDecimal valorApurado;

    @Column(precision = 12, scale = 2)
    private BigDecimal diferenca;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCaixa status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_abertura_id", nullable = false)
    private Usuario usuarioAbertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_fechamento_id")
    private Usuario usuarioFechamento;

    @Column(name = "aberto_em", nullable = false)
    private Instant abertoEm;

    @Column(name = "fechado_em")
    private Instant fechadoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        abertoEm = abertoEm == null ? now : abertoEm;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public Caixa() {}

    public UUID getId() { return id; }
    public LocalDate getDataCaixa() { return dataCaixa; }
    public void setDataCaixa(LocalDate dataCaixa) { this.dataCaixa = dataCaixa; }
    public BigDecimal getValorInicial() { return valorInicial; }
    public void setValorInicial(BigDecimal valorInicial) { this.valorInicial = valorInicial; }
    public BigDecimal getValorApurado() { return valorApurado; }
    public void setValorApurado(BigDecimal valorApurado) { this.valorApurado = valorApurado; }
    public BigDecimal getDiferenca() { return diferenca; }
    public void setDiferenca(BigDecimal diferenca) { this.diferenca = diferenca; }
    public StatusCaixa getStatus() { return status; }
    public void setStatus(StatusCaixa status) { this.status = status; }
    public Usuario getUsuarioAbertura() { return usuarioAbertura; }
    public void setUsuarioAbertura(Usuario usuarioAbertura) { this.usuarioAbertura = usuarioAbertura; }
    public Usuario getUsuarioFechamento() { return usuarioFechamento; }
    public void setUsuarioFechamento(Usuario usuarioFechamento) { this.usuarioFechamento = usuarioFechamento; }
    public Instant getAbertoEm() { return abertoEm; }
    public void setAbertoEm(Instant abertoEm) { this.abertoEm = abertoEm; }
    public Instant getFechadoEm() { return fechadoEm; }
    public void setFechadoEm(Instant fechadoEm) { this.fechadoEm = fechadoEm; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
