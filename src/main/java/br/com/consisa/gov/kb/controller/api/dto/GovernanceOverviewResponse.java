package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resposta do endpoint de overview gerencial de governança.
 *
 * @param totals      totais agregados de issues
 * @param bySystem    métricas agrupadas por sistema
 * @param generatedAt timestamp de geração do overview
 *
 * @author KB Governance Team
 * @since Sprint 5
 */
public record GovernanceOverviewResponse(
        OverviewTotals totals,
        List<SystemOverview> bySystem,
        OffsetDateTime generatedAt
) {

    /**
     * Totais agregados de issues de governança.
     */
    public record OverviewTotals(
            long open,
            long criticalOpen,
            long unassigned,
            long overdue
    ) {}

    /**
     * Métricas de governança por sistema.
     */
    public record SystemOverview(
            String system,
            String systemName,
            long openCount,
            long criticalCount,
            long overdueCount,
            long unassignedCount,
            int healthScore
    ) {}
}
