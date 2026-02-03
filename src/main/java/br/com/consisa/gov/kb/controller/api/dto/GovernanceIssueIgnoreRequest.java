package br.com.consisa.gov.kb.controller.api.dto;

public record GovernanceIssueIgnoreRequest(
        String reason,
        String actor
) {
}
