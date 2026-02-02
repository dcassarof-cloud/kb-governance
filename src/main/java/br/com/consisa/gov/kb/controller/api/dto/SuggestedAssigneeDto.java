package br.com.consisa.gov.kb.controller.api.dto;

public record SuggestedAssigneeDto(
        String agentId,
        String agentName,
        long pendingIssues,
        double score
) {
}
