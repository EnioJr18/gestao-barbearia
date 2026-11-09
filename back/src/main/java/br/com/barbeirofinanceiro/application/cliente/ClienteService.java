package br.com.barbeirofinanceiro.application.cliente;

import br.com.barbeirofinanceiro.domain.cliente.Cliente;
import br.com.barbeirofinanceiro.domain.cliente.ClienteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente criar(CriarClienteRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNome(normalizarNome(request.nome()));
        cliente.setTelefone(normalizarTelefone(request.telefone()));
        return clienteRepository.saveAndFlush(cliente);
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar(String nome, Boolean ativo) {
        Specification<Cliente> specification = (root, query, builder) -> builder.conjunction();
        if (nome != null && !nome.isBlank()) {
            String termo = "%" + nome.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("nome")), termo));
        }
        if (ativo != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("ativo"), ativo));
        }
        return clienteRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "nome"));
    }

    @Transactional(readOnly = true)
    public Cliente buscar(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException("Cliente não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ClienteResumoProjection> resumos() {
        return clienteRepository.buscarResumoVendas();
    }

    @Transactional(readOnly = true)
    public ClienteResumoProjection resumo(UUID id) {
        return resumos().stream()
                .filter(resumo -> resumo.clienteId().equals(id))
                .findFirst()
                .orElse(new ClienteResumoProjection(id, 0, java.math.BigDecimal.ZERO, null));
    }

    @Transactional
    public Cliente atualizar(UUID id, AtualizarClienteRequest request) {
        Cliente cliente = buscar(id);
        cliente.setNome(normalizarNome(request.nome()));
        cliente.setTelefone(normalizarTelefone(request.telefone()));
        return clienteRepository.saveAndFlush(cliente);
    }

    @Transactional
    public Cliente alterarAtivo(UUID id, boolean ativo) {
        Cliente cliente = buscar(id);
        cliente.setAtivo(ativo);
        return clienteRepository.saveAndFlush(cliente);
    }

    @Transactional
    public void excluir(UUID id) {
        Cliente cliente = buscar(id);
        try {
            clienteRepository.delete(cliente);
            clienteRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ClienteConflictException("Cliente possui pedidos vinculados e não pode ser excluído");
        }
    }

    private String normalizarNome(String nome) { return nome.trim(); }

    private String normalizarTelefone(String telefone) {
        return telefone == null || telefone.isBlank() ? null : telefone.trim();
    }
}
