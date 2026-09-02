package br.com.barbeirofinanceiro.application.despesa;

import br.com.barbeirofinanceiro.domain.categoria.Categoria;
import br.com.barbeirofinanceiro.domain.categoria.CategoriaRepository;
import br.com.barbeirofinanceiro.domain.categoria.TipoCategoria;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class CategoriaDespesaResolver {

    private final CategoriaRepository categoriaRepository;

    CategoriaDespesaResolver(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    Categoria resolver(UUID categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new DespesaNotFoundException("Categoria não encontrada"));

        if (categoria.getTipo() != TipoCategoria.DESPESA) {
            throw new DespesaValidationException("Categoria deve ser do tipo DESPESA");
        }

        return categoria;
    }
}
