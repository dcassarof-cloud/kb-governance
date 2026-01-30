package br.com.consisa.gov.kb.controller.api.dto;

public record GovernanceIssueStatusUpdateRequest(
        String status,
        String actor
) {
}
