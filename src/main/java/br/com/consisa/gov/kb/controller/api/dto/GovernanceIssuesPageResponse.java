package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

public record GovernanceIssuesPageResponse(
        List<GovernanceIssueResponse> items,
        int page,
        int size,
        long total
) {
}
