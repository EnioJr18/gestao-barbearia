package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import br.com.barbeirofinanceiro.application.auth.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest extends PostgresPersistenceTest {

    private static final String EMAIL = "jwt@teste.local";
    private static final String SENHA = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String token;

    @BeforeEach
    void prepararUsuario() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário JWT");
        usuario.setEmail(EMAIL);
        usuario.setSenhaHash(passwordEncoder.encode(SENHA));

        usuarioRepository.saveAndFlush(usuario);

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(EMAIL)
                        .password(usuario.getSenhaHash())
                        .roles("USER")
                        .build();

        token = jwtService.gerarToken(userDetails);
    }

    @AfterEach
    void limparUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void deveRetornarUnauthorizedQuandoNaoEnviarToken() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/atual"))
                .andDo(print());
    }

    @Test
    void devePermitirAcessoQuandoEnviarTokenValido() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/atual")
                        .header("Authorization", "Bearer " + token))
                .andDo(print());
    }

    @Test
    void deveRetornarUnauthorizedQuandoEnviarTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/caixas/atual")
                        .header(
                                "Authorization",
                                "Bearer token-invalido"
                        ))
                .andDo(print());
    }
}