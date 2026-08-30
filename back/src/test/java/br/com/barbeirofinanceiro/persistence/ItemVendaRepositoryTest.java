package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.ItemRepository;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.venda.ItemRelatorioProjection;
import br.com.barbeirofinanceiro.domain.venda.ItemVenda;
import br.com.barbeirofinanceiro.domain.venda.ItemVendaRepository;
import br.com.barbeirofinanceiro.domain.venda.StatusVenda;
import br.com.barbeirofinanceiro.domain.venda.Venda;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ItemVendaRepositoryTest extends PostgresPersistenceTest {

    @Autowired
    private ItemVendaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void deveAgruparItensDeVendasFinalizadasPorItemNoPeriodo() {
        Usuario usuario = criarUsuario();
        Caixa caixa = criarCaixa(usuario);

        Item corte = criarItem("Corte", TipoItem.SERVICO, BigDecimal.valueOf(30));
        Item pomada = criarItem("Pomada", TipoItem.PRODUTO, BigDecimal.valueOf(35));

        Venda venda1 = criarVenda(
                caixa,
                LocalDate.of(2026, 8, 23),
                BigDecimal.valueOf(95),
                StatusVenda.FINALIZADA
        );

        criarItemVenda(
                venda1,
                corte,
                2,
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(60)
        );

        criarItemVenda(
                venda1,
                pomada,
                1,
                BigDecimal.valueOf(35),
                BigDecimal.valueOf(35)
        );

        Venda venda2 = criarVenda(
                caixa,
                LocalDate.of(2026, 8, 24),
                BigDecimal.valueOf(30),
                StatusVenda.FINALIZADA
        );

        criarItemVenda(
                venda2,
                corte,
                1,
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(30)
        );

        Venda venda3 = criarVenda(
                caixa,
                LocalDate.of(2026, 8, 24),
                BigDecimal.valueOf(300),
                StatusVenda.CANCELADA
        );

        criarItemVenda(
                venda3,
                corte,
                10,
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(300)
        );

        entityManager.flush();

        List<ItemRelatorioProjection> resumo = repository.buscarResumoItens(
                LocalDate.of(2026, 8, 23),
                LocalDate.of(2026, 8, 24)
        );

        assertThat(resumo).hasSize(2);

        ItemRelatorioProjection itemCorte = resumo.stream()
                .filter(item -> item.nome().equals("Corte"))
                .findFirst()
                .orElseThrow();

        ItemRelatorioProjection itemPomada = resumo.stream()
                .filter(item -> item.nome().equals("Pomada"))
                .findFirst()
                .orElseThrow();

        assertThat(itemCorte.quantidade())
                .isEqualTo(3);

        assertThat(itemCorte.faturamento())
                .isEqualByComparingTo(BigDecimal.valueOf(90));

        assertThat(itemPomada.quantidade())
                .isEqualTo(1);

        assertThat(itemPomada.faturamento())
                .isEqualByComparingTo(BigDecimal.valueOf(35));

        assertThat(resumo.get(0).nome())
                .isEqualTo("Corte");

        assertThat(resumo.get(1).nome())
                .isEqualTo("Pomada");
    }

    private Usuario criarUsuario() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Item Relatório");
        usuario.setEmail(
                "item-relatorio-" + System.nanoTime() + "@teste.local"
        );
        usuario.setSenhaHash("hash");

        entityManager.persist(usuario);
        entityManager.flush();

        return usuario;
    }

    private Caixa criarCaixa(Usuario usuario) {
        Caixa caixa = newEntity(Caixa.class);
        caixa.setDataCaixa(LocalDate.of(2026, 8, 23));
        caixa.setValorInicial(BigDecimal.ZERO);
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setUsuarioAbertura(usuario);

        entityManager.persist(caixa);
        entityManager.flush();

        return caixa;
    }

    private Item criarItem(
            String nome,
            TipoItem tipo,
            BigDecimal preco
    ) {
        Item item = newEntity(Item.class);
        item.setNome(nome);
        item.setTipo(tipo);
        item.setPreco(preco);

        if (tipo == TipoItem.PRODUTO) {
            item.setEstoque(100);
        } else {
            item.setEstoque(null);
        }

        return itemRepository.saveAndFlush(item);
    }

    private Venda criarVenda(
            Caixa caixa,
            LocalDate dataVenda,
            BigDecimal valorTotal,
            StatusVenda status
    ) {
        Venda venda = newEntity(Venda.class);
        venda.setCaixa(caixa);
        venda.setDataVenda(dataVenda);
        venda.setValorTotal(valorTotal);
        venda.setStatus(status);

        entityManager.persist(venda);
        entityManager.flush();

        return venda;
    }

    private void criarItemVenda(
            Venda venda,
            Item item,
            int quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {
        ItemVenda itemVenda = newEntity(ItemVenda.class);
        itemVenda.setVenda(venda);
        itemVenda.setItem(item);
        itemVenda.setQuantidade(quantidade);
        itemVenda.setPrecoUnitario(precoUnitario);
        itemVenda.setSubtotal(subtotal);

        entityManager.persist(itemVenda);
    }
}