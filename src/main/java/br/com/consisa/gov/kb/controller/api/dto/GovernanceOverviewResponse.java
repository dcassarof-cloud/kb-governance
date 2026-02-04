package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record GovernanceOverviewResponse(
        Totals totals,
        List<SystemOverview> bySystem,
        OffsetDateTime generatedAt
) {
    public record Totals(
            long open,
            long criticalOpen,
            long unassigned,
            long overdue
    ) {
    }

    public record SystemOverview(
            String systemCode,
            String systemName,
            long openCount,
            long criticalCount,
            long overdueCount,
            long unassignedCount,
            double healthScore
    ) {
    }
}
