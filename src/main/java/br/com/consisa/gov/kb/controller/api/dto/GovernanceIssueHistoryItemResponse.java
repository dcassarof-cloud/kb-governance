package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record GovernanceIssueHistoryItemResponse(
        String action,
        String actor,
        OffsetDateTime createdAt,
        String oldValue,
        String newValue
) {
}
