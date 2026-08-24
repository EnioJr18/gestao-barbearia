package br.com.barbeirofinanceiro.domain.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    List<Item> findByTipoAndAtivo(TipoItem tipo, boolean ativo);
}
