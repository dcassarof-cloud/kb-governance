package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record GovernanceManagementDashboardResponse(
        long totalIssues,
        long openIssues,
        long overdueIssues,
        long unassignedIssues,
        List<StatusCount> issuesByStatus,
        List<SystemCount> issuesBySystem,
        List<TypeCount> issuesByType,
        long needsOpen,
        long needsRecurring,
        List<TopPriorityIssue> topCriticalIssues
) {
    public record StatusCount(String status, long total) {}

    public record SystemCount(String systemCode, String systemName, long total) {}

    public record TypeCount(String issueType, long total) {}

    public record TopPriorityIssue(
            Long id,
            String issueType,
            String severity,
            String status,
            String systemCode,
            String systemName,
            OffsetDateTime slaDueAt,
            boolean overdue,
            int priorityScore,
            String priorityLevel
    ) {}
}
