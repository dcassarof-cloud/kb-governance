package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record GovernanceDashboardResponse(
        Summary summary,
        List<TopRisk> topRisks,
        AttentionLists attentionLists,
        Trends trends,
        OffsetDateTime generatedAt
) {
    public record Summary(
            long open,
            long errorOpen,
            long warnOpen,
            long infoOpen,
            long overdue,
            long unassigned,
            double slaCompliancePercent
    ) {
    }

    public record TopRisk(
            String system,
            double healthScore,
            long errorCount,
            long overdueCount,
            long unassignedCount
    ) {
    }

    public record AttentionLists(
            List<OverdueIssue> overdueIssues,
            List<UnassignedIssue> unassignedIssues
    ) {
    }

    public record OverdueIssue(
            long id,
            String type,
            String system,
            String severity,
            OffsetDateTime slaDueAt
    ) {
    }

    public record UnassignedIssue(
            long id,
            String type,
            String system,
            String severity,
            OffsetDateTime createdAt
    ) {
    }

    public record Trends(
            Last7Days last7Days
    ) {
    }

    public record Last7Days(
            long opened,
            long resolved,
            long overdueVariation
    ) {
    }
}
