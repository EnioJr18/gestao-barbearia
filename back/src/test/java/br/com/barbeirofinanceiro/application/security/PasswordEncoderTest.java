package br.com.barbeirofinanceiro.application.security;

import br.com.barbeirofinanceiro.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder =
            new SecurityConfig().passwordEncoder();

    @Test
    void deveGerarHashDiferenteDaSenhaOriginal() {
        String senha = "123456";

        String hash = passwordEncoder.encode(senha);

        assertThat(hash)
                .isNotEqualTo(senha);
    }

    @Test
    void deveReconhecerSenhaCorreta() {
        String senha = "123456";
        String hash = passwordEncoder.encode(senha);

        assertThat(passwordEncoder.matches(senha, hash))
                .isTrue();
    }

    @Test
    void deveRejeitarSenhaIncorreta() {
        String senha = "123456";
        String hash = passwordEncoder.encode(senha);

        assertThat(passwordEncoder.matches("senha-errada", hash))
                .isFalse();
    }
}