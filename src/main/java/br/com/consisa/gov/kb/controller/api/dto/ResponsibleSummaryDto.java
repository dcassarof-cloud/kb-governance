package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record ResponsibleSummaryDto(
        String agentId,
        String agentName,
        long openIssues,
        Long overdueIssues,
        OffsetDateTime lastAssignedAt,
        Double avgResolutionDays
) {
}
