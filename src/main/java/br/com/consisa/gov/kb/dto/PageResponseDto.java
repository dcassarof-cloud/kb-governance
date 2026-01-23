package br.com.consisa.gov.kb.dto;

import java.util.List;

/**
 * Resposta paginada padrão do projeto.
 *
 * Observação:
 * - O front manda page=1 (1-based).
 * - Aqui devolvemos page igual ao que chegou (1-based) para evitar confusão no front.
 */
public record PageResponseDto<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<T> items
) {}
