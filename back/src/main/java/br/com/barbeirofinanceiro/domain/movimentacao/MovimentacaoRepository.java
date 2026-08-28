package br.com.barbeirofinanceiro.domain.movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, UUID>, JpaSpecificationExecutor<Movimentacao> {}
