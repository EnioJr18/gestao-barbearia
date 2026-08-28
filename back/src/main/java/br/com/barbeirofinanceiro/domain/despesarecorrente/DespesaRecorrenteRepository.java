package br.com.barbeirofinanceiro.domain.despesarecorrente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface DespesaRecorrenteRepository extends JpaRepository<DespesaRecorrente, UUID> {}
