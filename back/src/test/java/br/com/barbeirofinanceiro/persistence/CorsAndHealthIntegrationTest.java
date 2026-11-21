package br.com.barbeirofinanceiro.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "CORS_ALLOWED_ORIGINS=http://localhost:5173,https://app.example.test")
@AutoConfigureMockMvc
class CorsAndHealthIntegrationTest extends PostgresPersistenceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void permiteOrigemLocalEPreflightComAuthorization() throws Exception {
        preflight("http://localhost:5173")
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        containsStringIgnoringCase("authorization")))
                .andExpect(header().string("Access-Control-Allow-Origin", org.hamcrest.Matchers.not("*")));
    }

    @Test
    void permiteOrigemAdicionalConfigurada() throws Exception {
        preflight("https://app.example.test")
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.example.test"));
    }

    @Test
    void rejeitaOrigemNaoConfiguradaSemConcederCors() throws Exception {
        preflight("https://nao-autorizada.example.test")
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void readinessEPublicoMasNaoExponeDetalhesInternos() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void rotaOperacionalContinuaExigindoJwt() throws Exception {
        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions preflight(String origin) throws Exception {
        return mockMvc.perform(options("/api/v1/clientes")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"));
    }
}
