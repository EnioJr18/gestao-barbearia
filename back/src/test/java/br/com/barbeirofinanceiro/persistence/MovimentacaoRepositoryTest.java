package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.categoria.Categoria;
import br.com.barbeirofinanceiro.domain.categoria.TipoCategoria;
import br.com.barbeirofinanceiro.domain.movimentacao.DespesaResumoProjection;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import br.com.barbeirofinanceiro.domain.movimentacao.Movimentacao;
import br.com.barbeirofinanceiro.domain.movimentacao.MovimentacaoRepository;
import br.com.barbeirofinanceiro.domain.movimentacao.OrigemMovimentacao;
import br.com.barbeirofinanceiro.domain.movimentacao.TipoMovimentacao;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MovimentacaoRepositoryTest extends PostgresPersistenceTest {

    @Autowired
    private MovimentacaoRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deveCalcularResumoDasDespesasNoPeriodo() {
        String sufixo = String.valueOf(System.nanoTime());

        Categoria categoriaDespesa = criarCategoriaDespesa(sufixo);
        Categoria categoriaReceita = criarCategoriaReceita(sufixo);

        repository.save(movimentacao(
                TipoMovimentacao.DESPESA,
                BigDecimal.valueOf(100),
                LocalDate.of(2026, 8, 23),
                categoriaDespesa
        ));

        repository.save(movimentacao(
                TipoMovimentacao.DESPESA,
                BigDecimal.valueOf(50),
                LocalDate.of(2026, 8, 24),
                categoriaDespesa
        ));

        repository.save(movimentacao(
                TipoMovimentacao.DESPESA,
                BigDecimal.valueOf(200),
                LocalDate.of(2026, 8, 25),
                categoriaDespesa
        ));

        repository.save(movimentacao(
                TipoMovimentacao.RECEITA,
                BigDecimal.valueOf(500),
                LocalDate.of(2026, 8, 24),
                categoriaReceita
        ));

        repository.flush();

        DespesaResumoProjection resumo = repository.buscarResumoDespesas(
                LocalDate.of(2026, 8, 23),
                LocalDate.of(2026, 8, 24)
        );

        assertThat(resumo.despesas())
                .isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void deveRetornarZeroQuandoNaoHouverDespesasNoPeriodo() {
        String sufixo = String.valueOf(System.nanoTime());

        Categoria categoriaDespesa = criarCategoriaDespesa(sufixo);
        Categoria categoriaReceita = criarCategoriaReceita(sufixo);

        repository.save(movimentacao(
                TipoMovimentacao.DESPESA,
                BigDecimal.valueOf(200),
                LocalDate.of(2026, 8, 25),
                categoriaDespesa
        ));
        repository.save(movimentacao(
                TipoMovimentacao.RECEITA,
                BigDecimal.valueOf(500),
                LocalDate.of(2026, 8, 23),
                categoriaReceita
        ));
        repository.flush();

        DespesaResumoProjection resumo = repository.buscarResumoDespesas(
                LocalDate.of(2026, 8, 23),
                LocalDate.of(2026, 8, 24)
        );

        assertThat(resumo.despesas())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Categoria criarCategoriaDespesa(String sufixo) {
        Categoria categoria = newEntity(Categoria.class);
        categoria.setNome("Despesas Operacionais " + sufixo);
        categoria.setTipo(TipoCategoria.DESPESA);
        entityManager.persist(categoria);
        entityManager.flush();
        return categoria;
    }

    private Categoria criarCategoriaReceita(String sufixo) {
        Categoria categoria = newEntity(Categoria.class);
        categoria.setNome("Receitas " + sufixo);
        categoria.setTipo(TipoCategoria.RECEITA);
        entityManager.persist(categoria);
        entityManager.flush();
        return categoria;
    }



    private Movimentacao movimentacao(
            TipoMovimentacao tipo,
            BigDecimal valor,
            LocalDate data,
            Categoria categoria
    ) {
        Movimentacao movimentacao = newEntity(Movimentacao.class);
        movimentacao.setTipo(tipo);
        movimentacao.setOrigem(OrigemMovimentacao.MANUAL);
        movimentacao.setDescricao("Movimentacao teste");
        movimentacao.setValor(valor);
        movimentacao.setDataMovimentacao(data);
        movimentacao.setCategoria(categoria);
        movimentacao.setFormaPagamento(FormaPagamento.DINHEIRO);
        return movimentacao;
    }
}