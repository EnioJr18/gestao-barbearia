package br.com.barbeirofinanceiro.application.usuario;

import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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

        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuarioExistente -> {
            if (!usuarioExistente.getId().equals(usuario.getId())) {
                throw new UsuarioConflictException("Já existe um usuário com este e-mail");
            }
        });

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
        usuario.setAtivo(false);
        salvar(usuario);
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
