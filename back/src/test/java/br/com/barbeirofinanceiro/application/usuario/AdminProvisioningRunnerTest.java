package br.com.barbeirofinanceiro.application.usuario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProvisioningRunnerTest {

    @Mock
    private Environment environment;

    @Mock
    private AdminProvisioningService provisioningService;

    @InjectMocks
    private AdminProvisioningRunner runner;

    @Test
    void naoProvisionaQuandoConfirmacaoEstaAusente() {
        when(environment.getProperty("ADMIN_PROVISIONING_CONFIRM")).thenReturn(null);

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Provisionamento não confirmado; nenhuma conta foi alterada");

        verifyNoInteractions(provisioningService);
    }

    @Test
    void naoProvisionaQuandoConfirmacaoNaoEhTrue() {
        when(environment.getProperty("ADMIN_PROVISIONING_CONFIRM")).thenReturn("sim");

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Provisionamento não confirmado; nenhuma conta foi alterada");

        verifyNoInteractions(provisioningService);
    }
}
