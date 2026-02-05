package br.com.consisa.gov.kb.controller.api.dto;

public record GovernanceWorkloadResponse(
        String responsibleId,
        long openIssues,
        long overdueIssues,
        double avgResolutionTimeHours,
        long systemsHandled
) {
}
