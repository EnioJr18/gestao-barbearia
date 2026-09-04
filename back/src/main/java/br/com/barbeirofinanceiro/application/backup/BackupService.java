package br.com.barbeirofinanceiro.application.backup;

import br.com.barbeirofinanceiro.domain.backup.BackupExecucao;
import br.com.barbeirofinanceiro.domain.backup.BackupExecucaoRepository;
import br.com.barbeirofinanceiro.domain.backup.BackupStatus;
import br.com.barbeirofinanceiro.domain.backup.BackupTipo;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackupService {

    private final BackupExecucaoRepository repository;
    private final BackupStorage storage;
    private final PostgresBackupExecutor postgresBackupExecutor;
    private final RestauracaoMetadataStore metadataStore;
    private final ManutencaoCoordinator maintenance;

    public BackupService(
            BackupExecucaoRepository repository,
            BackupStorage storage,
            PostgresBackupExecutor postgresBackupExecutor,
            RestauracaoMetadataStore metadataStore,
            ManutencaoCoordinator maintenance
    ) {
        this.repository = repository;
        this.storage = storage;
        this.postgresBackupExecutor = postgresBackupExecutor;
        this.metadataStore = metadataStore;
        this.maintenance = maintenance;
    }

    public BackupResponse criar() {
        return maintenance.executarBackup(this::criarInterno);
    }

    public List<BackupResponse> listar() {
        List<Path> arquivos = storage.listar();
        Map<String, BackupExecucao> execucoes = porArquivo(arquivos.stream().map(path -> path.getFileName().toString()).toList());
        return arquivos.stream()
                .filter(path -> execucoes.containsKey(path.getFileName().toString()))
                .map(path -> new BackupResponse(
                        path.getFileName().toString(),
                        storage.ultimaModificacao(path),
                        storage.tamanho(path),
                        execucoes.get(path.getFileName().toString()).getStatus()
                ))
                .toList();
    }

    public RestauracaoResponse restaurar(String arquivo) {
        maintenance.validarAplicacaoDisponivel();
        Path backup = storage.resolverExistente(arquivo);
        if (!repository.existsByArquivoAndTipoInAndStatus(
                arquivo,
                List.of(BackupTipo.MANUAL, BackupTipo.AUTOMATICO),
                BackupStatus.SUCESSO
        )) {
            throw new BackupNotFoundException("Arquivo de backup não encontrado");
        }
        postgresBackupExecutor.validarBackup(backup);
        return maintenance.executarRestauracao(() -> restaurarInterno(arquivo, backup));
    }

    private BackupResponse criarInterno() {
        String arquivo = storage.gerarNomeArquivo();
        BackupExecucao execucao = iniciar(BackupTipo.MANUAL, arquivo);
        Path temporario = null;
        try {
            temporario = storage.criarTemporario();
            postgresBackupExecutor.criarBackup(temporario);
            validarArquivoGerado(temporario);
            postgresBackupExecutor.validarBackup(temporario);
            Path publicado = storage.publicar(temporario, arquivo);
            execucao.setStatus(BackupStatus.SUCESSO);
            execucao.setTamanhoBytes(storage.tamanho(publicado));
            execucao.setFimEm(Instant.now());
            repository.saveAndFlush(execucao);
            return new BackupResponse(arquivo, execucao.getFimEm(), execucao.getTamanhoBytes(), execucao.getStatus());
        } catch (RuntimeException exception) {
            storage.excluirSilenciosamente(temporario);
            falhar(execucao, exception);
            throw exception;
        }
    }

    private RestauracaoResponse restaurarInterno(String arquivo, Path backup) {
        BackupExecucao inicio = iniciar(BackupTipo.RESTAURACAO, arquivo);
        metadataStore.registrar(arquivo, BackupStatus.INICIADO.name(), null);
        try {
            postgresBackupExecutor.restaurarBackup(backup);
            maintenance.marcarReinicioNecessario();
            registrarSucessoRestauracao(arquivo);
            metadataStore.registrar(arquivo, BackupStatus.SUCESSO.name(), null);
            return new RestauracaoResponse(
                    arquivo,
                    Instant.now(),
                    true,
                    "Restauração concluída. Reinicie o sistema pelo ambiente de execução antes de continuar."
            );
        } catch (RuntimeException exception) {
            falhar(inicio, exception);
            metadataStore.registrar(arquivo, BackupStatus.FALHA.name(), mensagemSegura(exception));
            throw exception;
        }
    }

    private BackupExecucao iniciar(BackupTipo tipo, String arquivo) {
        return repository.saveAndFlush(BackupExecucao.iniciar(tipo, arquivo));
    }

    private void registrarSucessoRestauracao(String arquivo) {
        BackupExecucao execucao = BackupExecucao.iniciar(BackupTipo.RESTAURACAO, arquivo);
        execucao.setStatus(BackupStatus.SUCESSO);
        execucao.setFimEm(Instant.now());
        repository.saveAndFlush(execucao);
    }

    private void falhar(BackupExecucao execucao, RuntimeException exception) {
        execucao.setStatus(BackupStatus.FALHA);
        execucao.setFimEm(Instant.now());
        execucao.setErro(mensagemSegura(exception));
        repository.saveAndFlush(execucao);
    }

    private void validarArquivoGerado(Path arquivo) {
        storage.validarTamanhoArquivo(arquivo);
    }

    private Map<String, BackupExecucao> porArquivo(Collection<String> arquivos) {
        Map<String, BackupExecucao> porArquivo = new HashMap<>();
        repository.findByArquivoInAndTipoInAndStatusOrderByInicioEmDesc(
                        arquivos,
                        List.of(BackupTipo.MANUAL, BackupTipo.AUTOMATICO),
                        BackupStatus.SUCESSO
                )
                .forEach(execucao -> porArquivo.putIfAbsent(execucao.getArquivo(), execucao));
        return porArquivo;
    }

    private String mensagemSegura(RuntimeException exception) {
        return exception instanceof BackupExecutionException
                ? exception.getMessage()
                : "Falha inesperada na operação de backup";
    }
}
