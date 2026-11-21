package br.com.barbeirofinanceiro.application.usuario;

import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioPerfil;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Locale;
import java.util.UUID;

@Service
public class AdminProvisioningService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioAdminLock adminLock;
    private final Validator validator;

    public AdminProvisioningService(
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder,
            UsuarioAdminLock adminLock,
            Validator validator
    ) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.adminLock = adminLock;
        this.validator = validator;
    }

    @Transactional
    public void criarPrimeiroAdmin(String nome, String email, String senha) {
        validarDados(nome, email, senha);
        adminLock.bloquearAlteracoesAdministrativas();
        exigirAusenciaDeAdminAtivo();

        Usuario usuario = Usuario.novo();
        usuario.setNome(nome.trim());
        usuario.setEmail(normalizarEmail(email));
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(UsuarioPerfil.ADMIN);
        usuario.setAtivo(true);
        salvarSemExporDados(usuario);
    }

    @Transactional
    public void promoverContaSelecionada(UUID id) {
        adminLock.bloquearAlteracoesAdministrativas();
        exigirAusenciaDeAdminAtivo();

        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException("Conta selecionada não encontrada"));
        if (!usuario.isAtivo()) {
            throw new UsuarioValidationException("A conta selecionada precisa estar ativa");
        }
        usuario.setPerfil(UsuarioPerfil.ADMIN);
        salvarSemExporDados(usuario);
    }

    private void exigirAusenciaDeAdminAtivo() {
        if (usuarios.countByPerfilAndAtivoTrue(UsuarioPerfil.ADMIN) > 0) {
            throw new UsuarioConflictException("Já existe um administrador ativo");
        }
    }

    private void validarDados(String nome, String email, String senha) {
        if (senha == null || senha.length() < 12 || senha.length() > 255) {
            throw new UsuarioValidationException("Dados de provisionamento inválidos");
        }
        var request = new CriarUsuarioRequest(nome, email, senha, UsuarioPerfil.ADMIN);
        if (!validator.validate(request).isEmpty()) {
            throw new UsuarioValidationException("Dados de provisionamento inválidos");
        }
    }

    private void salvarSemExporDados(Usuario usuario) {
        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new UsuarioConflictException("Não foi possível provisionar a conta administrativa");
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
