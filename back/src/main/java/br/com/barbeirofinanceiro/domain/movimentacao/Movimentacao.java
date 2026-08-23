package br.com.barbeirofinanceiro.domain.movimentacao;

import br.com.barbeirofinanceiro.domain.categoria.Categoria;
import br.com.barbeirofinanceiro.domain.despesarecorrente.DespesaRecorrente;
import br.com.barbeirofinanceiro.domain.servico.Servico;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "movimentacoes")
public class Movimentacao {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigemMovimentacao origem;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_movimentacao", nullable = false)
    private LocalDate dataMovimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    @Column(name = "nome_servico_snapshot", length = 120)
    private String nomeServicoSnapshot;

    @Column(name = "valor_servico_snapshot", precision = 12, scale = 2)
    private BigDecimal valorServicoSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "despesa_recorrente_id")
    private DespesaRecorrente despesaRecorrente;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void prePersist() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
    protected Movimentacao() {}

    public UUID getId() { return id; }
    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }
    public OrigemMovimentacao getOrigem() { return origem; }
    public void setOrigem(OrigemMovimentacao origem) { this.origem = origem; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getDataMovimentacao() { return dataMovimentacao; }
    public void setDataMovimentacao(LocalDate dataMovimentacao) { this.dataMovimentacao = dataMovimentacao; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public Servico getServico() { return servico; }
    public void setServico(Servico servico) { this.servico = servico; }
    public String getNomeServicoSnapshot() { return nomeServicoSnapshot; }
    public void setNomeServicoSnapshot(String nomeServicoSnapshot) { this.nomeServicoSnapshot = nomeServicoSnapshot; }
    public BigDecimal getValorServicoSnapshot() { return valorServicoSnapshot; }
    public void setValorServicoSnapshot(BigDecimal valorServicoSnapshot) { this.valorServicoSnapshot = valorServicoSnapshot; }
    public DespesaRecorrente getDespesaRecorrente() { return despesaRecorrente; }
    public void setDespesaRecorrente(DespesaRecorrente despesaRecorrente) { this.despesaRecorrente = despesaRecorrente; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
