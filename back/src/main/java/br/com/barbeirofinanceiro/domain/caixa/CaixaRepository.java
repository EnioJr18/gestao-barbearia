package br.com.barbeirofinanceiro.domain.caixa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface CaixaRepository extends JpaRepository<Caixa, UUID> {
    Optional<Caixa> findFirstByStatusOrderByDataCaixaDesc(StatusCaixa status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caixa c where c.status = br.com.barbeirofinanceiro.domain.caixa.StatusCaixa.ABERTO order by c.dataCaixa desc")
    Optional<Caixa> findOpenForUpdate();
}
