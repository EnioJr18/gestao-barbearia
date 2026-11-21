package br.com.barbeirofinanceiro;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class ProvisionarAdminApplication {

    private ProvisionarAdminApplication() {
    }

    public static void main(String[] args) {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(BarbeiroFinanceiroApplication.class)
                .profiles("admin-provisioning")
                .web(WebApplicationType.NONE)
                .run(args)) {
            // The provisioning runner performs the one-shot operation before the context closes.
        }
    }
}
