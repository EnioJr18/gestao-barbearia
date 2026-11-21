package br.com.barbeirofinanceiro.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    long countByPerfilAndAtivoTrue(UsuarioPerfil perfil);

    List<Usuario> findAllByOrderByNomeAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usuario from Usuario usuario where usuario.perfil = :perfil and usuario.ativo = true order by usuario.id")
    List<Usuario> findAtivosPorPerfilParaAtualizacao(@Param("perfil") UsuarioPerfil perfil);
}
