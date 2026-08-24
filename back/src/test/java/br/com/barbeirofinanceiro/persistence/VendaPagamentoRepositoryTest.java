package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.venda.Venda;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamento;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamentoRepository;
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
class VendaPagamentoRepositoryTest extends PostgresPersistenceTest {
    @Autowired
    private VendaPagamentoRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void devePersistirPagamentoVinculadoAVenda() {
        Venda venda = venda();
        VendaPagamento pagamento = pagamento(venda, FormaPagamento.PIX, BigDecimal.valueOf(50));

        VendaPagamento salvo = repository.saveAndFlush(pagamento);

        assertThat(repository.findById(salvo.getId())).get().satisfies(recuperado -> {
            assertThat(recuperado.getVenda().getId()).isEqualTo(venda.getId());
            assertThat(recuperado.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        });
    }

    @Test
    void devePersistirMultiplosMeiosDePagamentoNaMesmaVenda() {
        Venda venda = venda();
        repository.save(pagamento(venda, FormaPagamento.DINHEIRO, BigDecimal.valueOf(20)));
        repository.save(pagamento(venda, FormaPagamento.CARTAO_CREDITO, BigDecimal.valueOf(30)));

        repository.flush();

        assertThat(repository.findByVendaId(venda.getId()))
                .extracting(VendaPagamento::getFormaPagamento)
                .containsExactlyInAnyOrder(FormaPagamento.DINHEIRO, FormaPagamento.CARTAO_CREDITO);
    }

    private Venda venda() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Pagamento");
        usuario.setEmail("pagamento-" + System.nanoTime() + "@teste.local");
        usuario.setSenhaHash("hash");
        entityManager.persist(usuario);
        entityManager.flush();

        Caixa caixa = newEntity(Caixa.class);
        caixa.setDataCaixa(LocalDate.of(2026, 8, 23));
        caixa.setValorInicial(BigDecimal.ZERO);
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setUsuarioAbertura(usuario);
        entityManager.persist(caixa);
        entityManager.flush();

        Venda venda = newEntity(Venda.class);
        venda.setCaixa(caixa);
        venda.setDataVenda(LocalDate.of(2026, 8, 23));
        venda.setValorTotal(BigDecimal.valueOf(50));
        entityManager.persist(venda);
        entityManager.flush();
        return venda;
    }

    private VendaPagamento pagamento(Venda venda, FormaPagamento forma, BigDecimal valor) {
        VendaPagamento pagamento = newEntity(VendaPagamento.class);
        pagamento.setVenda(venda);
        pagamento.setFormaPagamento(forma);
        pagamento.setValor(valor);
        return pagamento;
    }
}
