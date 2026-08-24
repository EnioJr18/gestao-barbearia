package br.com.barbeirofinanceiro.domain.venda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendaPagamentoRepository extends JpaRepository<VendaPagamento, UUID> {
    List<VendaPagamento> findByVendaId(UUID vendaId);
}
