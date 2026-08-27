package br.com.barbeirofinanceiro.domain.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {
    List<Item> findByTipoAndAtivo(TipoItem tipo, boolean ativo);

    Optional<Item> findByNomeIgnoreCase(String nome);

}
