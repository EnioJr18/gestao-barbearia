package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.cliente.Cliente;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.venda.Venda;
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import br.com.barbeirofinanceiro.domain.venda.StatusVenda;
import br.com.barbeirofinanceiro.domain.venda.VendaResumoProjection;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class VendaRepositoryTest extends PostgresPersistenceTest {
    @Autowired
    private VendaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void devePersistirVendaComClienteOpcional() {
        Caixa caixa = caixa();
        Venda venda = venda(caixa);

        Venda salva = repository.saveAndFlush(venda);

        assertThat(repository.findById(salva.getId())).get().satisfies(recuperada -> {
            assertThat(recuperada.getCaixa().getId()).isEqualTo(caixa.getId());
            assertThat(recuperada.getCliente()).isNull();
        });
    }

    @Test
    void devePersistirVendaVinculadaACliente() {
        Caixa caixa = caixa();
        Cliente cliente = newEntity(Cliente.class);
        cliente.setNome("Cliente da Venda");
        entityManager.persist(cliente);
        entityManager.flush();

        Venda venda = venda(caixa);
        venda.setCliente(cliente);
        Venda salva = repository.saveAndFlush(venda);

        assertThat(repository.findById(salva.getId())).get().extracting(Venda::getCliente).isNotNull();
    }

    private Caixa caixa() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Caixa");
        usuario.setEmail("venda-" + System.nanoTime() + "@teste.local");
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
        return caixa;
    }

    private Venda venda(Caixa caixa) {
        Venda venda = newEntity(Venda.class);
        venda.setCaixa(caixa);
        venda.setDataVenda(LocalDate.of(2026, 8, 23));
        venda.setValorTotal(BigDecimal.valueOf(50));
        venda.setStatus(StatusVenda.FINALIZADA);
        return venda;
    }

    @Test
    void deveCalcularResumoApenasComVendasFinalizadasNoPeriodo() {
        Caixa caixa = caixa();

        Venda venda1 = venda(caixa);
        venda1.setValorTotal(BigDecimal.valueOf(30));
        repository.save(venda1);

        Venda venda2 = venda(caixa);
        venda2.setValorTotal(BigDecimal.valueOf(20));
        repository.save(venda2);

        Venda cancelada = venda(caixa);
        cancelada.setValorTotal(BigDecimal.valueOf(100));
        cancelada.setStatus(StatusVenda.CANCELADA);
        repository.saveAndFlush(cancelada);

        VendaResumoProjection resumo = repository.findResumoByDataVendaBetween(
                LocalDate.of(2026, 8, 23),
                LocalDate.of(2026, 8, 23)
        );

        assertThat(resumo.faturamento())
                .isEqualByComparingTo(BigDecimal.valueOf(50));

        assertThat(resumo.quantidadeVendas())
                .isEqualTo(2);
    }
}
