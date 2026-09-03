package br.com.barbeirofinanceiro.domain.backup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BackupExecucaoRepository extends JpaRepository<BackupExecucao, UUID> {

    List<BackupExecucao> findByArquivoInOrderByInicioEmDesc(Collection<String> arquivos);
}
