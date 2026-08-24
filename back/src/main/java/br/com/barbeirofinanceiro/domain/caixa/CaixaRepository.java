package br.com.barbeirofinanceiro.domain.caixa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CaixaRepository extends JpaRepository<Caixa, UUID> {
    Optional<Caixa> findFirstByStatusOrderByDataCaixaDesc(StatusCaixa status);
}
