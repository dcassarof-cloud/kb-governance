package br.com.consisa.gov.kb.controller.api.dto;

public record GovernanceIssueAssignResponsibleRequest(
        String responsibleId,
        String responsibleType,
        String responsibleName,
        String actor
) {
}
