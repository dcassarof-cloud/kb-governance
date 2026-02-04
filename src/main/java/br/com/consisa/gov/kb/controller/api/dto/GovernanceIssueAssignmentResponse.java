package br.com.consisa.gov.kb.controller.api.dto;

import br.com.consisa.gov.kb.dto.GovernanceLabelDto;

import java.time.OffsetDateTime;

public record GovernanceIssueAssignmentResponse(
        Long id,
        Long issueId,
        String agentId,
        String agentName,
        GovernanceLabelDto status,
        OffsetDateTime assignedAt,
        OffsetDateTime dueDate
) {
}
