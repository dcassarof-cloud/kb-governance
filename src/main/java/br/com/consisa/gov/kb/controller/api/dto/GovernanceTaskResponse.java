package br.com.consisa.gov.kb.controller.api.dto;

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
        String status,
        String riskLevel,
        String priority,
        String assigneeType,
        String assigneeId,
        OffsetDateTime dueAt,
        OffsetDateTime lastActionAt,
        List<String> issues
) {
}
