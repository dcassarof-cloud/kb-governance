package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

/**
 * DTO genérico de resposta paginada.
 *
 * Formato esperado pelo front:
 * {
 *   "data": [...],
 *   "page": 1,
 *   "size": 10,
 *   "total": 1103,
 *   "totalPages": 111
 * }
 *
 * IMPORTANTE: page é 1-based (igual ao front)
 */
public record PaginatedResponse<T>(
        List<T> data,
        int page,
        int size,
        long total,
        int totalPages
) {
    public static <T> PaginatedResponse<T> from(org.springframework.data.domain.Page<T> pageResult,
                                                int page1Based,
                                                int size) {
        int safePage = Math.max(1, page1Based);
        int safeSize = Math.max(1, size);
        return new PaginatedResponse<>(
                pageResult.getContent(),
                safePage,
                safeSize,
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
