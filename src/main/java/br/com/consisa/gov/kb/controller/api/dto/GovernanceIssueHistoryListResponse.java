package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

public record GovernanceIssueHistoryListResponse(
        List<GovernanceIssueHistoryItemResponse> items
) {
}
