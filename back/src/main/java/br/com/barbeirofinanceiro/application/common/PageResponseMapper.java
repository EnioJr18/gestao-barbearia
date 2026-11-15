package br.com.barbeirofinanceiro.application.common;

import org.springframework.data.domain.Page;

import java.util.function.Function;

public final class PageResponseMapper {

    private PageResponseMapper() {
    }

    public static <S, T> PageResponse<T> from(Page<S> source, Function<S, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast()
        );
    }
}
