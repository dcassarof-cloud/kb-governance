package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

public record SuggestedAssigneeResponse(
        SuggestedAssigneeDto suggested,
        List<SuggestedAssigneeDto> others
) {
}
