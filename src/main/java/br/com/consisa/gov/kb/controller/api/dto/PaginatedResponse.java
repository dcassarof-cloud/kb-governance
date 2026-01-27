package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

/**
 * DTO genérico de resposta paginada.
 * 
 * Formato esperado pelo front:
 * {
 *   "page": 1,
 *   "size": 10,
 *   "totalElements": 1103,
 *   "totalPages": 111,
 *   "items": [...]
 * }
 * 
 * IMPORTANTE: page é 1-based (igual ao front)
 */
public record PaginatedResponse<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<T> items
) {
}
