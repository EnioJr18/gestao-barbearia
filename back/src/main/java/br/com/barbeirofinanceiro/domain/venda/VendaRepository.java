package br.com.barbeirofinanceiro.domain.venda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface VendaRepository extends JpaRepository<Venda, UUID>, JpaSpecificationExecutor<Venda> {
}
