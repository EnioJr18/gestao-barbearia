package br.com.barbeirofinanceiro.domain.categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {}
