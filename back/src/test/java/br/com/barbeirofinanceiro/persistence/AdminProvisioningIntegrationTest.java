package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.application.usuario.AdminProvisioningService;
import br.com.barbeirofinanceiro.application.usuario.UsuarioConflictException;
import br.com.barbeirofinanceiro.application.usuario.UsuarioNotFoundException;
import br.com.barbeirofinanceiro.application.usuario.UsuarioValidationException;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioPerfil;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AdminProvisioningIntegrationTest extends PostgresPersistenceTest {

    @Autowired private AdminProvisioningService provisioning;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ApplicationContext context;

    @BeforeEach
    void limparAntes() {
        usuarios.deleteAll();
    }

    @AfterEach
    void limparDepois() {
        usuarios.deleteAll();
    }

    @Test
    void criaAdminEmBancoSemUsuariosEArmazenaSomenteHashBcrypt() {
        provisioning.criarPrimeiroAdmin("Primeiro Admin", "primeiro@teste.local", "SenhaProvisionada123!");

        Usuario usuario = usuarios.findByEmailIgnoreCase("primeiro@teste.local").orElseThrow();
        assertThat(usuario.getPerfil()).isEqualTo(UsuarioPerfil.ADMIN);
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(passwordEncoder.matches("SenhaProvisionada123!", usuario.getSenhaHash())).isTrue();
        assertThat(usuario.getSenhaHash()).startsWith("$2");
    }

    @Test
    void promoveSomenteContaAtivaSelecionadaPorId() {
        Usuario escolhido = criarUsuario("Escolhido", "escolhido@teste.local", true);
        criarUsuario("Outra conta", "outra@teste.local", true);

        provisioning.promoverContaSelecionada(escolhido.getId());

        assertThat(usuarios.findById(escolhido.getId()).orElseThrow().getPerfil())
                .isEqualTo(UsuarioPerfil.ADMIN);
        assertThat(usuarios.findByEmailIgnoreCase("outra@teste.local").orElseThrow().getPerfil())
                .isEqualTo(UsuarioPerfil.OPERADOR);
    }

    @Test
    void recusaProvisionamentoQuandoJaExisteAdminAtivo() {
        Usuario existente = criarUsuario("Admin", "admin-existente@teste.local", true);
        existente.setPerfil(UsuarioPerfil.ADMIN);
        usuarios.saveAndFlush(existente);

        assertThatThrownBy(() -> provisioning.criarPrimeiroAdmin(
                "Outro", "outro@teste.local", "SenhaProvisionada123!"))
                .isInstanceOf(UsuarioConflictException.class);
    }

    @Test
    void recusaContaInexistenteInativaEParametrosInvalidos() {
        assertThatThrownBy(() -> provisioning.promoverContaSelecionada(UUID.randomUUID()))
                .isInstanceOf(UsuarioNotFoundException.class);

        Usuario inativo = criarUsuario("Inativo", "inativo@teste.local", false);
        assertThatThrownBy(() -> provisioning.promoverContaSelecionada(inativo.getId()))
                .isInstanceOf(UsuarioValidationException.class);
        assertThatThrownBy(() -> provisioning.criarPrimeiroAdmin(" ", "email-invalido", "curta"))
                .isInstanceOf(UsuarioValidationException.class);
    }

    @Test
    void runnerDeProvisionamentoNaoFazParteDaInicializacaoNormal() {
        assertThat(context.containsBean("adminProvisioningRunner")).isFalse();
    }

    private Usuario criarUsuario(String nome, String email, boolean ativo) {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode("SenhaOperador123!"));
        usuario.setAtivo(ativo);
        usuario.setPerfil(UsuarioPerfil.OPERADOR);
        return usuarios.saveAndFlush(usuario);
    }
}
