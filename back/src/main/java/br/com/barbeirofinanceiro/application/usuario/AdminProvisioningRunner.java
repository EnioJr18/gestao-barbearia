package br.com.barbeirofinanceiro.application.usuario;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("admin-provisioning")
public class AdminProvisioningRunner implements ApplicationRunner {

    private final Environment environment;
    private final AdminProvisioningService provisioningService;

    public AdminProvisioningRunner(
            Environment environment,
            AdminProvisioningService provisioningService
    ) {
        this.environment = environment;
        this.provisioningService = provisioningService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"true".equalsIgnoreCase(environment.getProperty("ADMIN_PROVISIONING_CONFIRM"))) {
            throw new IllegalStateException("Provisionamento não confirmado; nenhuma conta foi alterada");
        }

        String action = required("ADMIN_PROVISIONING_ACTION");
        if ("create".equalsIgnoreCase(action)) {
            String name = required("ADMIN_PROVISIONING_NAME");
            String email = required("ADMIN_PROVISIONING_EMAIL");
            String password = required("ADMIN_PROVISIONING_PASSWORD");
            if (password.length() < 12 || password.length() > 255) {
                throw new IllegalArgumentException("A senha de provisionamento deve ter entre 12 e 255 caracteres");
            }
            provisioningService.criarPrimeiroAdmin(name, email, password);
        } else if ("promote".equalsIgnoreCase(action)) {
            UUID id;
            try {
                id = UUID.fromString(required("ADMIN_PROVISIONING_USER_ID"));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Identificador selecionado inválido");
            }
            provisioningService.promoverContaSelecionada(id);
        } else {
            throw new IllegalArgumentException("Ação de provisionamento inválida");
        }

        System.out.println("Provisionamento administrativo concluído. Remova as variáveis temporárias do ambiente.");
    }

    private String required(String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Variável obrigatória ausente: " + name);
        }
        return value;
    }
}
