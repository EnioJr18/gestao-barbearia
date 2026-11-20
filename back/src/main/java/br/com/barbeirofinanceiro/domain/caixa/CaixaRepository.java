package br.com.barbeirofinanceiro.domain.caixa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface CaixaRepository extends JpaRepository<Caixa, UUID>, JpaSpecificationExecutor<Caixa> {
    Optional<Caixa> findFirstByStatusOrderByDataCaixaDesc(StatusCaixa status);

    @Override
    @EntityGraph(attributePaths = {"usuarioAbertura", "usuarioFechamento"})
    Page<Caixa> findAll(Specification<Caixa> specification, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caixa c where c.status = br.com.barbeirofinanceiro.domain.caixa.StatusCaixa.ABERTO order by c.dataCaixa desc")
    Optional<Caixa> findOpenForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caixa c where c.id = :id")
    Optional<Caixa> findByIdForUpdate(@Param("id") UUID id);
}
