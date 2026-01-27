package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

/**
 * DTO para item da lista de artigos.
 * 
 * Formato esperado pelo front:
 * {
 *   "id": 123,
 *   "title": "Como cadastrar cliente",
 *   "slug": "como-cadastrar-cliente",
 *   "manualLink": "https://...",
 *   "systemCode": "CONSISANET",
 *   "systemName": "ConsisaNET",
 *   "governanceStatus": "PENDING",
 *   "updatedAt": "2024-01-19T10:30:00Z"
 * }
 */
public record ArticleListResponse(
        Long id,
        String title,
        String slug,
        String manualLink,
        String systemCode,
        String systemName,
        String governanceStatus,
        OffsetDateTime updatedAt
) {
}
