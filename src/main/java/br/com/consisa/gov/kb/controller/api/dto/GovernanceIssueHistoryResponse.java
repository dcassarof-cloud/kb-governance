package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record GovernanceIssueHistoryResponse(
        Long id,
        Long issueId,
        String action,
        String oldValue,
        String newValue,
        String actor,
        OffsetDateTime createdAt
) {
}
