package br.com.barbeirofinanceiro.application.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void deveRejeitarSecretAusente() {
        assertThatThrownBy(() -> new JwtProperties(null, 3_600_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("jwt.secret é obrigatório");
    }

    @Test
    void deveRejeitarSecretCurto() {
        assertThatThrownBy(() -> new JwtProperties("curto", 3_600_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("jwt.secret deve possuir ao menos 32 bytes");
    }

    @Test
    void deveRejeitarExpiracaoNaoPositiva() {
        assertThatThrownBy(() -> new JwtProperties("chave-secreta-de-teste-com-tamanho-suficiente", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("jwt.expiration deve ser maior que zero");
    }
}
