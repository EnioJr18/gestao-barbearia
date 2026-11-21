package br.com.barbeirofinanceiro.config;

import br.com.barbeirofinanceiro.application.auth.JwtAuthenticationFilter;
import br.com.barbeirofinanceiro.application.auth.JwtProperties;
import br.com.barbeirofinanceiro.application.backup.ManutencaoFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(Environment environment) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origensPermitidas(environment));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static List<String> origensPermitidas(Environment environment) {
        String configuracao = environment.getProperty("CORS_ALLOWED_ORIGINS");
        boolean producao = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod")
                        || profile.equalsIgnoreCase("production"));

        if (configuracao == null || configuracao.isBlank()) {
            if (producao) {
                throw new IllegalStateException(
                        "CORS_ALLOWED_ORIGINS deve ser configurada no perfil de produção");
            }
            return List.of("http://localhost:5173");
        }

        List<String> origens = Arrays.stream(configuracao.split(",", -1))
                .map(String::trim)
                .toList();
        if (origens.isEmpty() || origens.stream().anyMatch(String::isBlank)) {
            throw new IllegalStateException(
                    "CORS_ALLOWED_ORIGINS deve conter origens explícitas separadas por vírgula");
        }

        origens.forEach(origem -> {
            URI uri = validarOrigem(origem);
            if (producao && hostLocal(uri.getHost())) {
                throw new IllegalStateException(
                        "CORS_ALLOWED_ORIGINS não pode usar localhost no perfil de produção");
            }
        });
        if (!producao) {
            LinkedHashSet<String> origensDesenvolvimento = new LinkedHashSet<>();
            origensDesenvolvimento.add("http://localhost:5173");
            origensDesenvolvimento.addAll(origens);
            return List.copyOf(origensDesenvolvimento);
        }
        return List.copyOf(origens);
    }

    private static URI validarOrigem(String origem) {
        try {
            URI uri = new URI(origem);
            if (origem.contains("*")
                    || uri.getScheme() == null
                    || !List.of("http", "https").contains(uri.getScheme().toLowerCase(Locale.ROOT))
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getPort() == 0
                    || uri.getPort() > 65_535) {
                throw new IllegalStateException(
                        "CORS_ALLOWED_ORIGINS aceita somente origens HTTP ou HTTPS explícitas, sem wildcard");
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(
                    "CORS_ALLOWED_ORIGINS contém uma origem inválida", exception);
        }
    }

    private static boolean hostLocal(String host) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return normalizedHost.equals("localhost")
                || normalizedHost.equals("localhost.localdomain")
                || normalizedHost.equals("::1")
                || normalizedHost.equals("[::1]")
                || normalizedHost.startsWith("127.");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ManutencaoFilter manutencaoFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeSecurityError(response, HttpStatus.UNAUTHORIZED, "Autenticação necessária"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeSecurityError(response, HttpStatus.FORBIDDEN, "Acesso negado"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/readiness").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/me/senha").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/usuarios/me/desativar").authenticated()
                        .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/backups/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(manutencaoFilter, AuthorizationFilter.class);

        return http.build();
    }

    private void writeSecurityError(
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
