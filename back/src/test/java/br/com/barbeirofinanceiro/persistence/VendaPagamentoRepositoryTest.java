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
import br.com.barbeirofinanceiro.domain.venda.PagamentoResumoProjection;
import br.com.barbeirofinanceiro.domain.venda.StatusVenda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    @Test
    void deveAgruparPagamentosPorFormaEmVendasFinalizadasNoPeriodo() {
        Usuario usuario = usuario();
        Caixa caixa = caixa(usuario);

        Venda vendaFinalizada1 = venda(caixa, LocalDate.of(2026, 8, 23), BigDecimal.valueOf(100), StatusVenda.FINALIZADA);
        repository.save(pagamento(vendaFinalizada1, FormaPagamento.DINHEIRO, BigDecimal.valueOf(60)));
        repository.save(pagamento(vendaFinalizada1, FormaPagamento.PIX, BigDecimal.valueOf(40)));

        Venda vendaFinalizada2 = venda(caixa, LocalDate.of(2026, 8, 24), BigDecimal.valueOf(50), StatusVenda.FINALIZADA);
        repository.save(pagamento(vendaFinalizada2, FormaPagamento.CARTAO_CREDITO, BigDecimal.valueOf(50)));

        Venda vendaCancelada = venda(caixa, LocalDate.of(2026, 8, 24), BigDecimal.valueOf(200), StatusVenda.CANCELADA);
        repository.save(pagamento(vendaCancelada, FormaPagamento.DINHEIRO, BigDecimal.valueOf(200)));

        repository.flush();

        List<PagamentoResumoProjection> resumo =
                repository.buscarResumoPorFormaPagamento(
                        LocalDate.of(2026, 8, 23),
                        LocalDate.of(2026, 8, 24)
                );

        PagamentoResumoProjection dinheiro = resumo.stream()
                .filter(item -> item.formaPagamento() == FormaPagamento.DINHEIRO)
                .findFirst()
                .orElseThrow();

        PagamentoResumoProjection pix = resumo.stream()
                .filter(item -> item.formaPagamento() == FormaPagamento.PIX)
                .findFirst()
                .orElseThrow();

        PagamentoResumoProjection cartaoCredito = resumo.stream()
                .filter(item -> item.formaPagamento() == FormaPagamento.CARTAO_CREDITO)
                .findFirst()
                .orElseThrow();

        assertThat(dinheiro.valor())
                .isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(pix.valor())
                .isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(cartaoCredito.valor())
                .isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(resumo).hasSize(3);
    }

    private Usuario usuario() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Pagamento");
        usuario.setEmail("pagamento-" + System.nanoTime() + "@teste.local");
        usuario.setSenhaHash("hash");
        entityManager.persist(usuario);
        entityManager.flush();
        return usuario;
    }

    private Caixa caixa(Usuario usuario) {
        Caixa caixa = newEntity(Caixa.class);
        caixa.setDataCaixa(LocalDate.of(2026, 8, 23));
        caixa.setValorInicial(BigDecimal.ZERO);
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setUsuarioAbertura(usuario);
        entityManager.persist(caixa);
        entityManager.flush();
        return caixa;
    }

    private Venda venda() {
        return venda(caixa(usuario()), LocalDate.of(2026, 8, 23), BigDecimal.valueOf(50), StatusVenda.FINALIZADA);
    }

    private Venda venda(Caixa caixa, LocalDate dataVenda, BigDecimal valorTotal, StatusVenda status) {
        Venda venda = newEntity(Venda.class);
        venda.setCaixa(caixa);
        venda.setDataVenda(dataVenda);
        venda.setValorTotal(valorTotal);
        venda.setStatus(status);
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
