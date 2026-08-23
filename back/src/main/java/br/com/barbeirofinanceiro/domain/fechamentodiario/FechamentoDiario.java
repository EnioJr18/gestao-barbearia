package br.com.barbeirofinanceiro.domain.fechamentodiario;

import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fechamentos_diarios")
public class FechamentoDiario {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(length = 1000)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fechado_em", nullable = false)
    private Instant fechadoEm;

    @PrePersist void prePersist() { fechadoEm = Instant.now(); }
    protected FechamentoDiario() {}

    public UUID getId() { return id; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Instant getFechadoEm() { return fechadoEm; }
}
