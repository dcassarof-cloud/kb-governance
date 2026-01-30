package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record GovernanceIssueAssignmentResponse(
        Long id,
        Long issueId,
        String agentId,
        String agentName,
        String status,
        OffsetDateTime assignedAt,
        OffsetDateTime dueDate
) {
}
