package br.com.barbeirofinanceiro.application.usuario;

import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioPerfil;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioAdminLock adminLock;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            UsuarioAdminLock adminLock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminLock = adminLock;
    }

    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new UsuarioConflictException("Já existe um usuário com este e-mail");
        }

        Usuario usuario = Usuario.novo();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(request.perfil() == null ? UsuarioPerfil.OPERADOR : request.perfil());
        if (usuario.getPerfil() == UsuarioPerfil.ADMIN) {
            adminLock.bloquearAlteracoesAdministrativas();
        }
        return UsuarioResponse.from(salvar(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarAdministrativamente() {
        return usuarioRepository.findAllByOrderByNomeAsc().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse consultarAdministrativamente(UUID id) {
        return UsuarioResponse.from(buscarPorId(id));
    }

    @Transactional
    public UsuarioResponse atualizarAdministrativamente(
            UUID id,
            AtualizarUsuarioAdministrativoRequest request
    ) {
        adminLock.bloquearAlteracoesAdministrativas();
        Usuario usuario = buscarPorId(id);
        validarEmailDisponivel(request.email(), id);

        if (usuario.isAtivo() && usuario.getPerfil() == UsuarioPerfil.ADMIN
                && request.perfil() != UsuarioPerfil.ADMIN) {
            exigirOutroAdminAtivo();
        }

        usuario.setNome(request.nome().trim());
        usuario.setEmail(normalizarEmail(request.email()));
        usuario.setPerfil(request.perfil());
        return UsuarioResponse.from(salvar(usuario));
    }

    @Transactional
    public UsuarioResponse ativarAdministrativamente(UUID id) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(true);
        return UsuarioResponse.from(salvar(usuario));
    }

    @Transactional
    public void desativarAdministrativamente(UUID id) {
        adminLock.bloquearAlteracoesAdministrativas();
        Usuario usuario = buscarPorId(id);
        if (usuario.isAtivo() && usuario.getPerfil() == UsuarioPerfil.ADMIN) {
            exigirOutroAdminAtivo();
        }
        usuario.setAtivo(false);
        salvar(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse consultarAtual(Authentication authentication) {
        return UsuarioResponse.from(usuarioAutenticado(authentication));
    }

    @Transactional
    public UsuarioResponse atualizarAtual(
            Authentication authentication,
            AtualizarUsuarioRequest request
    ) {
        Usuario usuario = usuarioAutenticado(authentication);
        String email = normalizarEmail(request.email());

        validarEmailDisponivel(email, usuario.getId());

        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        return UsuarioResponse.from(salvar(usuario));
    }

    @Transactional
    public void alterarSenhaAtual(Authentication authentication, AlterarSenhaRequest request) {
        Usuario usuario = usuarioAutenticado(authentication);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new UsuarioValidationException("Não foi possível alterar a senha");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        salvar(usuario);
    }

    @Transactional
    public void desativarAtual(Authentication authentication) {
        Usuario usuario = usuarioAutenticado(authentication);
        desativarAdministrativamente(usuario.getId());
    }

    private Usuario buscarPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException("Usuário não encontrado"));
    }

    private void validarEmailDisponivel(String email, UUID idIgnorado) {
        usuarioRepository.findByEmailIgnoreCase(normalizarEmail(email)).ifPresent(usuarioExistente -> {
            if (!usuarioExistente.getId().equals(idIgnorado)) {
                throw new UsuarioConflictException("Já existe um usuário com este e-mail");
            }
        });
    }

    private void exigirOutroAdminAtivo() {
        int administradoresAtivos = usuarioRepository
                .findAtivosPorPerfilParaAtualizacao(UsuarioPerfil.ADMIN).size();
        if (administradoresAtivos <= 1) {
            throw new UsuarioConflictException("Não é possível remover o último administrador ativo");
        }
    }

    private Usuario usuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new UsuarioNotFoundException("Usuário autenticado não encontrado");
        }

        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsuarioNotFoundException("Usuário autenticado não encontrado"));
    }

    private Usuario salvar(Usuario usuario) {
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            throw new UsuarioConflictException("Já existe um usuário com este e-mail");
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
