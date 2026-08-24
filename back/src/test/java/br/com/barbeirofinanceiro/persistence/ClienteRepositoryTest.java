package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.cliente.Cliente;
import br.com.barbeirofinanceiro.domain.cliente.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ClienteRepositoryTest extends PostgresPersistenceTest {
    @Autowired
    private ClienteRepository repository;

    @Test
    void devePersistirERecuperarClientePeloId() {
        Cliente cliente = newEntity(Cliente.class);
        cliente.setNome("Cliente Persistido");
        cliente.setTelefone("85999990000");

        Cliente salvo = repository.saveAndFlush(cliente);

        assertThat(salvo.getId()).isNotNull();
        assertThat(repository.findById(salvo.getId()))
                .get()
                .satisfies(recuperado -> {
                    assertThat(recuperado.getNome()).isEqualTo("Cliente Persistido");
                    assertThat(recuperado.getTelefone()).isEqualTo("85999990000");
                });
    }

    @Test
    void devePermitirClienteSemTelefone() {
        Cliente cliente = newEntity(Cliente.class);
        cliente.setNome("Cliente Sem Telefone");

        Cliente salvo = repository.saveAndFlush(cliente);

        assertThat(repository.findById(salvo.getId())).get().extracting(Cliente::getTelefone).isNull();
    }

}
