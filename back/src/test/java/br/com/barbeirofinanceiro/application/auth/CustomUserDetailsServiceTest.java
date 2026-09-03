package br.com.barbeirofinanceiro.application.auth;

import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void deveRetornarUserDetailsQuandoUsuarioEncontradoEAtivo() {
        Usuario usuario = mock(Usuario.class);

        when(usuario.getEmail())
                .thenReturn("usuario@teste.local");

        when(usuario.getSenhaHash())
                .thenReturn("hash");

        when(usuario.isAtivo())
                .thenReturn(true);

        when(usuarioRepository.findByEmailIgnoreCase("usuario@teste.local"))
                .thenReturn(Optional.of(usuario));

        UserDetails userDetails =
                service.loadUserByUsername("usuario@teste.local");

        assertEquals(
                "usuario@teste.local",
                userDetails.getUsername()
        );

        assertEquals(
                "hash",
                userDetails.getPassword()
        );

        assertTrue(
                userDetails.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_USER"))
        );

        assertTrue(userDetails.isEnabled());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findByEmailIgnoreCase("usuario@teste.local"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("usuario@teste.local")
        );
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioInativo() {
        Usuario usuario = mock(Usuario.class);

        when(usuario.isAtivo())
                .thenReturn(false);

        when(usuarioRepository.findByEmailIgnoreCase("usuario@teste.local"))
                .thenReturn(Optional.of(usuario));

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("usuario@teste.local")
        );
    }
}
