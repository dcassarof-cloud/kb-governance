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
 *   "message": "Conteúdo vazio",
 *   "createdAt": "2024-01-19T10:30:00Z",
 *   "updatedAt": "2024-01-20T10:30:00Z",
 *   "assignedAgentId": "123",
 *   "assignedAgentName": "Maria Silva",
 *   "dueDate": "2024-02-01T00:00:00Z"
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
        String message,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String assignedAgentId,
        String assignedAgentName,
        OffsetDateTime dueDate,
        String details,
        String responsibleId,
        String responsibleType,
        OffsetDateTime slaDueAt,
        OffsetDateTime resolvedAt,
        String ignoredReason,
        String typeDisplayName,
        String typeDescription,
        String typeRecommendation
) {
}
