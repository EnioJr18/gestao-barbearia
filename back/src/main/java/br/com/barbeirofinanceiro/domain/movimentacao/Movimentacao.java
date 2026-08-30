package br.com.barbeirofinanceiro.domain.movimentacao;

import br.com.barbeirofinanceiro.domain.categoria.Categoria;
import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.despesarecorrente.DespesaRecorrente;
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
    @JoinColumn(name = "caixa_id")
    private Caixa caixa;

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
    public Movimentacao() {}

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
    public Caixa getCaixa() { return caixa; }
    public void setCaixa(Caixa caixa) { this.caixa = caixa; }
    public DespesaRecorrente getDespesaRecorrente() { return despesaRecorrente; }
    public void setDespesaRecorrente(DespesaRecorrente despesaRecorrente) { this.despesaRecorrente = despesaRecorrente; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
