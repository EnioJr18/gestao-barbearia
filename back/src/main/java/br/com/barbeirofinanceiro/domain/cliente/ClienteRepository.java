package br.com.barbeirofinanceiro.domain.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID>, JpaSpecificationExecutor<Cliente> {
}
