package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record GovernanceIssueAssignRequest(
        String agentId,
        String agentName,
        OffsetDateTime dueDate,
        String actor
) {
}
