package br.com.barbeirofinanceiro.application.item;

import br.com.barbeirofinanceiro.domain.item.TipoItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AtualizarItemRequest {
    @NotBlank(message = "nome é obrigatório")
    private String nome;

    @NotNull(message = "tipo é obrigatório")
    private TipoItem tipo;

    @NotNull(message = "preco é obrigatório")
    @DecimalMin(value = "0.00", message = "preco deve ser maior ou igual a zero")
    private BigDecimal preco;

    @DecimalMin(value = "0", message = "estoque deve ser maior ou igual a zero")
    private Integer estoque;

    private boolean estoqueInformado;

    public AtualizarItemRequest() {
    }

    public String nome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoItem tipo() {
        return tipo;
    }

    public void setTipo(TipoItem tipo) {
        this.tipo = tipo;
    }

    public BigDecimal preco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Integer estoque() {
        return estoque;
    }

    public void setEstoque(Integer estoque) {
        this.estoque = estoque;
        this.estoqueInformado = true;
    }

    public boolean estoqueInformado() {
        return estoqueInformado;
    }
}
