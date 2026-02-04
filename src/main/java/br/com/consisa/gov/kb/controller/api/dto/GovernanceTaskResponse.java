package br.com.consisa.gov.kb.controller.api.dto;

import br.com.consisa.gov.kb.dto.GovernanceLabelDto;

import java.time.OffsetDateTime;
import java.util.List;

public record GovernanceTaskResponse(
        Long taskId,
        Long articleId,
        String title,
        String slug,
        String manualLink,
        String systemCode,
        String systemName,
        GovernanceLabelDto status,
        GovernanceLabelDto riskLevel,
        GovernanceLabelDto priority,
        GovernanceLabelDto assigneeType,
        String assigneeId,
        OffsetDateTime dueAt,
        OffsetDateTime lastActionAt,
        List<GovernanceLabelDto> issues
) {
}
