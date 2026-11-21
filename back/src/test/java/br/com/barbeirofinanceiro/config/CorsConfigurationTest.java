package br.com.barbeirofinanceiro.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigurationTest {

    @Test
    void usaOrigemLocalComoPadraoForaDeProducao() {
        assertThat(SecurityConfig.origensPermitidas(new MockEnvironment()))
                .containsExactly("http://localhost:5173");
    }

    @Test
    void aceitaMultiplasOrigensSeparadasPorVirgulaSemWildcard() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CORS_ALLOWED_ORIGINS", "http://localhost:5173, https://app.example.test");

        assertThat(SecurityConfig.origensPermitidas(environment))
                .containsExactly("http://localhost:5173", "https://app.example.test")
                .doesNotContain("*");
    }

    @Test
    void preservaOrigemLocalAoAdicionarOrigemDeDesenvolvimento() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CORS_ALLOWED_ORIGINS", "https://app.example.test");

        assertThat(SecurityConfig.origensPermitidas(environment))
                .containsExactly("http://localhost:5173", "https://app.example.test");
    }

    @Test
    void falhaEmProducaoQuandoNaoHaOrigensConfiguradas() {
        new ApplicationContextRunner()
                .withUserConfiguration(CorsBeanConfiguration.class)
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("CORS_ALLOWED_ORIGINS deve ser configurada no perfil de produção");
                });
    }

    @Test
    void rejeitaWildcardOrigemComPathEDelimitadorAlternativo() {
        for (String origins : new String[]{"*", "https://*.example.test", "https://app.example.test/path",
                "http://localhost:5173;https://app.example.test"}) {
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("CORS_ALLOWED_ORIGINS", origins);
            assertThatThrownBy(() -> SecurityConfig.origensPermitidas(environment))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void rejeitaOrigemLocalNoPerfilDeProducao() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CORS_ALLOWED_ORIGINS", "http://localhost:5173,https://app.example.test");
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> SecurityConfig.origensPermitidas(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
    }

    @Configuration(proxyBeanMethods = false)
    static class CorsBeanConfiguration {
        @Bean
        CorsConfigurationSource corsConfigurationSource(Environment environment) {
            return new SecurityConfig().corsConfigurationSource(environment);
        }
    }
}
