package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

/**
 * DTO detalhado para issue de governança (Sprint 5).
 *
 * <p>Inclui campos de SLA, responsável direto e metadados do tipo.
 *
 * @author KB Governance Team
 * @since Sprint 5
 */
public record GovernanceIssueDetailResponse(
        // Identificação
        Long id,
        String issueType,
        String severity,
        String status,

        // Artigo relacionado
        Long articleId,
        String articleTitle,
        String systemCode,
        String systemName,

        // Detalhes
        String message,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,

        // Responsável (assignment legado)
        String assignedAgentId,
        String assignedAgentName,
        OffsetDateTime dueDate,

        // Responsável direto (Sprint 5)
        String responsibleId,
        String responsibleType,

        // SLA (Sprint 5)
        OffsetDateTime slaDueAt,
        OffsetDateTime resolvedAt,
        String ignoredReason,
        boolean overdue,

        // Metadados do tipo (Sprint 5)
        String typeDisplayName,
        String typeDescription,
        String typeRecommendation,

        // Detalhes extras
        String details
) {

    /**
     * Construtor de compatibilidade que converte do formato antigo.
     */
    public static GovernanceIssueDetailResponse fromLegacy(
            GovernanceIssueResponse legacy,
            String responsibleId,
            String responsibleType,
            OffsetDateTime slaDueAt,
            OffsetDateTime resolvedAt,
            String ignoredReason,
            boolean overdue,
            String typeDisplayName,
            String typeDescription,
            String typeRecommendation
    ) {
        return new GovernanceIssueDetailResponse(
                legacy.id(),
                legacy.issueType(),
                legacy.severity(),
                legacy.status(),
                legacy.articleId(),
                legacy.articleTitle(),
                legacy.systemCode(),
                legacy.systemName(),
                legacy.message(),
                legacy.createdAt(),
                legacy.updatedAt(),
                legacy.assignedAgentId(),
                legacy.assignedAgentName(),
                legacy.dueDate(),
                responsibleId,
                responsibleType,
                slaDueAt,
                resolvedAt,
                ignoredReason,
                overdue,
                typeDisplayName,
                typeDescription,
                typeRecommendation,
                legacy.details()
        );
    }
}
