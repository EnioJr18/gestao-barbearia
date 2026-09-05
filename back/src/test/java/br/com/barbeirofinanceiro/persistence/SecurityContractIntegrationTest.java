package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.application.auth.JwtService;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityContractIntegrationTest extends PostgresPersistenceTest {

    private static final String EMAIL = "security-contract@teste.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String tokenValido;

    @BeforeEach
    void prepararUsuario() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Security Contract");
        usuario.setEmail(EMAIL);
        usuario.setSenhaHash(passwordEncoder.encode("123456"));
        usuarioRepository.saveAndFlush(usuario);

        tokenValido = jwtService.gerarToken(
                User.withUsername(EMAIL)
                        .password(usuario.getSenhaHash())
                        .roles("USER")
                        .build()
        );
    }

    @AfterEach
    void limparUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void deveRetornarUnauthorizedEmRotaProtegidaSemToken() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/atual"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarUnauthorizedEmRotaProtegidaComTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/atual")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devePermitirAcessoEmRotaProtegidaComTokenValido() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/atual")
                        .header("Authorization", "Bearer " + tokenValido))
                .andExpect(status().isNotFound());
    }

    @Test
    void devePermitirPreflightDoFrontendVite() throws Exception {
        mockMvc.perform(options("/api/v1/caixas/atual")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
