package br.com.barbeirofinanceiro.application.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        service = new AuthenticationService(
                authenticationManager,
                jwtService
        );
    }

    @Test
    void deveAutenticarUsuarioEGerarToken() {
        LoginRequest request = new LoginRequest(
                "usuario@teste.local",
                "123456"
        );

        UserDetails userDetails = User
                .withUsername("usuario@teste.local")
                .password("hash")
                .roles("USER")
                .build();

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.senha()
                )
        )).thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(userDetails);

        when(jwtService.gerarToken(userDetails))
                .thenReturn("token-jwt");

        LoginResponse response = service.autenticar(request);

        assertThat(response.token())
                .isEqualTo("token-jwt");

        assertThat(response.tipo())
                .isEqualTo("Bearer");

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.senha()
                )
        );

        verify(jwtService).gerarToken(userDetails);
    }

    @Test
    void devePropagarExcecaoQuandoAutenticacaoFalhar() {
        LoginRequest request = new LoginRequest(
                "usuario@teste.local",
                "senha-incorreta"
        );

        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.senha()
                )
        )).thenThrow(
                new org.springframework.security.authentication.BadCredentialsException(
                        "Credenciais inválidas"
                )
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.autenticar(request)
                )
                .isInstanceOf(
                        org.springframework.security.authentication.BadCredentialsException.class
                );
    }
}