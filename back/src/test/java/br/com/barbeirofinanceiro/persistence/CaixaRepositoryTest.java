package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CaixaRepositoryTest extends PostgresPersistenceTest {
    @Autowired
    private CaixaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void devePersistirERecuperarCaixaAberto() {
        Usuario usuario = usuario("abertura@teste.local");
        Caixa caixa = caixa(LocalDate.of(2026, 8, 23), usuario);

        Caixa salvo = repository.saveAndFlush(caixa);

        assertThat(repository.findFirstByStatusOrderByDataCaixaDesc(StatusCaixa.ABERTO))
                .get()
                .satisfies(recuperado -> {
                    assertThat(recuperado.getId()).isEqualTo(salvo.getId());
                    assertThat(recuperado.getStatus()).isEqualTo(StatusCaixa.ABERTO);
                });
    }

    @Test
    void deveRespeitarIndiceParcialDeUmUnicoCaixaAberto() {
        Usuario usuario = usuario("unicidade@teste.local");
        repository.saveAndFlush(caixa(LocalDate.of(2026, 8, 23), usuario));
        Caixa segundoCaixa = caixa(LocalDate.of(2026, 8, 24), usuario);

        repository.save(segundoCaixa);

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("uk_caixas_aberto");
    }

    private Usuario usuario(String email) {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário de Teste");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash");
        entityManager.persist(usuario);
        entityManager.flush();
        return usuario;
    }

    private Caixa caixa(LocalDate data, Usuario usuario) {
        Caixa caixa = newEntity(Caixa.class);
        caixa.setDataCaixa(data);
        caixa.setValorInicial(BigDecimal.valueOf(100));
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setUsuarioAbertura(usuario);
        return caixa;
    }
}
