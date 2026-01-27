package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

/**
 * DTO para grupo de artigos duplicados.
 * 
 * Formato esperado pelo front:
 * {
 *   "contentHash": "abc123...",
 *   "count": 3,
 *   "articleIds": [123, 456, 789]
 * }
 */
public record DuplicateGroupResponse(
        String contentHash,
        int count,
        List<Long> articleIds
) {
}
