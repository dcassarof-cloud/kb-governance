package br.com.consisa.gov.kb.controller.api.dto;

import br.com.consisa.gov.kb.dto.GovernanceLabelDto;

public record GovernanceIssueStatusResponse(
        Long id,
        GovernanceLabelDto status
) {
}
