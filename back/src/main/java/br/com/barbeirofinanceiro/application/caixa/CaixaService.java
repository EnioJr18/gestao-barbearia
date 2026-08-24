package br.com.barbeirofinanceiro.application.caixa;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class CaixaService {
    private final CaixaRepository caixaRepository;
    private final UsuarioRepository usuarioRepository;

    public CaixaService(CaixaRepository caixaRepository, UsuarioRepository usuarioRepository) {
        this.caixaRepository = caixaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Caixa abrir(BigDecimal valorInicial, Authentication authentication) {
        if (valorInicial == null || valorInicial.signum() <= 0) {
            throw new CaixaValidationException("valorInicial deve ser maior que zero");
        }

        if (caixaRepository.findFirstByStatusOrderByDataCaixaDesc(StatusCaixa.ABERTO).isPresent()) {
            throw new CaixaConflictException("Já existe um caixa aberto");
        }

        Caixa caixa = new Caixa();
        caixa.setDataCaixa(LocalDate.now());
        caixa.setValorInicial(valorInicial);
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setUsuarioAbertura(resolveAuthenticatedUser(authentication));

        try {
            return caixaRepository.saveAndFlush(caixa);
        } catch (DataIntegrityViolationException exception) {
            throw new CaixaConflictException("Já existe um caixa aberto");
        }
    }

    @Transactional(readOnly = true)
    public Caixa atual() {
        return caixaRepository.findFirstByStatusOrderByDataCaixaDesc(StatusCaixa.ABERTO)
                .orElseThrow(() -> new CaixaNotFoundException("Nenhum caixa aberto encontrado"));
    }

    @Transactional
    public Caixa fechar(UUID id, BigDecimal valorApurado, Authentication authentication) {
        if (valorApurado == null || valorApurado.signum() < 0) {
            throw new CaixaValidationException("valorApurado deve ser maior ou igual a zero");
        }

        Caixa caixa = caixaRepository.findById(id)
                .orElseThrow(() -> new CaixaNotFoundException("Caixa não encontrado"));
        if (caixa.getStatus() != StatusCaixa.ABERTO) {
            throw new CaixaConflictException("Caixa já está fechado");
        }

        caixa.setValorApurado(valorApurado);
        caixa.setDiferenca(valorApurado.subtract(caixa.getValorInicial()));
        caixa.setStatus(StatusCaixa.FECHADO);
        caixa.setUsuarioFechamento(resolveAuthenticatedUser(authentication));
        caixa.setFechadoEm(Instant.now());
        return caixaRepository.saveAndFlush(caixa);
    }

    private Usuario resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticatedUserException("Usuário autenticado é obrigatório");
        }

        String name = authentication.getName();
        try {
            return usuarioRepository.findById(UUID.fromString(name))
                    .orElseThrow(() -> new AuthenticatedUserException("Usuário autenticado não encontrado"));
        } catch (IllegalArgumentException ignored) {
            return usuarioRepository.findByEmailIgnoreCase(name)
                    .orElseThrow(() -> new AuthenticatedUserException("Usuário autenticado não encontrado"));
        }
    }
}
