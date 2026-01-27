package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

/**
 * DTO para issue de governança.
 * 
 * Formato esperado pelo front:
 * {
 *   "id": 1,
 *   "issueType": "INCOMPLETE_CONTENT",
 *   "severity": "WARN",
 *   "status": "OPEN",
 *   "articleId": 123,
 *   "articleTitle": "Como cadastrar cliente",
 *   "systemCode": "CONSISANET",
 *   "systemName": "ConsisaNET",
 *   "details": "Conteúdo vazio",
 *   "createdAt": "2024-01-19T10:30:00Z"
 * }
 */
public record GovernanceIssueResponse(
        Long id,
        String issueType,
        String severity,
        String status,
        Long articleId,
        String articleTitle,
        String systemCode,
        String systemName,
        String details,
        OffsetDateTime createdAt
) {
}
