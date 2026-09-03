package br.com.barbeirofinanceiro.application.backup;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ManutencaoFilter extends OncePerRequestFilter {

    private final ManutencaoCoordinator coordinator;

    public ManutencaoFilter(ManutencaoCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.GET.matches(request.getMethod()) || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri.equals("/api/v1/auth/login")
                || (HttpMethod.POST.matches(request.getMethod())
                && uri.matches("/api/v1/backups/[^/]+/restaurar"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            coordinator.executarEscrita(() -> {
                try {
                    filterChain.doFilter(request, response);
                    return null;
                } catch (IOException | ServletException exception) {
                    throw new ManutencaoServletException(exception);
                }
            });
        } catch (ManutencaoServletException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw (ServletException) cause;
        } catch (BackupConflictException exception) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Sistema em manutenção para restauração de backup\"}");
        }
    }

    private static final class ManutencaoServletException extends RuntimeException {
        private ManutencaoServletException(Exception cause) { super(cause); }
    }
}
