package br.com.barbeirofinanceiro.application.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public LoginResponse autenticar(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.senha()
                )
        );

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        UserDetails userDetails = userDetailsService.loadUserByUsername(principal.getUsername());
        String token = jwtService.gerarToken(userDetails);

        return new LoginResponse(token, "Bearer");
    }
}
