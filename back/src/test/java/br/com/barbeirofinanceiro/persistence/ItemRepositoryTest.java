package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.ItemRepository;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ItemRepositoryTest extends PostgresPersistenceTest {
    @Autowired
    private ItemRepository repository;

    @Test
    void devePersistirServicoComEstoqueNulo() {
        Item servico = item("Corte", TipoItem.SERVICO, BigDecimal.valueOf(40), null);

        Item salvo = repository.saveAndFlush(servico);

        assertThat(salvo.getId()).isNotNull();
        assertThat(repository.findById(salvo.getId())).get().satisfies(item -> {
            assertThat(item.getTipo()).isEqualTo(TipoItem.SERVICO);
            assertThat(item.getEstoque()).isNull();
        });
    }

    @Test
    void devePersistirProdutoComEstoqueNaoNegativo() {
        Item produto = item("Pomada", TipoItem.PRODUTO, BigDecimal.valueOf(35), 10);

        Item salvo = repository.saveAndFlush(produto);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getEstoque()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void deveBuscarItensPorTipoEAtivo() {
        repository.saveAndFlush(item("Servico Ativo", TipoItem.SERVICO, BigDecimal.TEN, null));
        Item inativo = item("Servico Inativo", TipoItem.SERVICO, BigDecimal.TEN, null);
        inativo.setAtivo(false);
        repository.saveAndFlush(inativo);

        assertThat(repository.findByTipoAndAtivo(TipoItem.SERVICO, true))
                .extracting(Item::getNome)
                .containsExactly("Servico Ativo");
    }

    private Item item(String nome, TipoItem tipo, BigDecimal preco, Integer estoque) {
        Item item = newEntity(Item.class);
        item.setNome(nome);
        item.setTipo(tipo);
        item.setPreco(preco);
        item.setEstoque(estoque);
        return item;
    }
}
