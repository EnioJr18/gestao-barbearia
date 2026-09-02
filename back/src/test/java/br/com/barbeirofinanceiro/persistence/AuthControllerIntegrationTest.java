package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends PostgresPersistenceTest {

    private static final String EMAIL_VALIDO = "usuario@teste.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararUsuarioValido() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Teste");
        usuario.setEmail(EMAIL_VALIDO);
        usuario.setSenhaHash(passwordEncoder.encode("123456"));
        usuarioRepository.saveAndFlush(usuario);
    }

    @AfterEach
    void limparUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void deveAutenticarUsuarioComCredenciaisValidas() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "usuario@teste.local",
                                "senha": "123456"
                            }
                            """))
                .andDo(result -> {
                    System.out.println("STATUS: " + result.getResponse().getStatus());
                    System.out.println("BODY: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    void deveRetornarUnauthorizedQuandoSenhaForIncorreta() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "usuario@teste.local",
                                "senha": "senha-errada"
                            }
                            """))
                .andDo(result -> {
                    System.out.println("STATUS: " + result.getResponse().getStatus());
                    System.out.println("BODY: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("E-mail ou senha inválidos"));
    }

    @Test
    void deveRetornarUnauthorizedQuandoUsuarioNaoExistir() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "inexistente@teste.local",
                                "senha": "123456"
                            }
                            """))
                .andDo(result -> {
                    System.out.println("STATUS: " + result.getResponse().getStatus());
                    System.out.println("BODY: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("E-mail ou senha inválidos"));
    }

    @Test
    void deveRetornarBadRequestQuandoEmailForInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "email-invalido",
                                "senha": "123456"
                            }
                            """))
                .andDo(result -> {
                    System.out.println("STATUS: " + result.getResponse().getStatus());
                    System.out.println("BODY: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isBadRequest());
    }
}
