package br.com.barbeirofinanceiro.domain.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import br.com.barbeirofinanceiro.application.cliente.ClienteResumoProjection;

import java.util.List;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID>, JpaSpecificationExecutor<Cliente> {
    @Query("""
        SELECT new br.com.barbeirofinanceiro.application.cliente.ClienteResumoProjection(
            c.id, COUNT(v.id), COALESCE(SUM(v.valorTotal), 0), MAX(v.dataVenda)
        )
        FROM Cliente c
        LEFT JOIN Venda v ON v.cliente.id = c.id
            AND v.status = br.com.barbeirofinanceiro.domain.venda.StatusVenda.FINALIZADA
        GROUP BY c.id
        """)
    List<ClienteResumoProjection> buscarResumoVendas();
}
