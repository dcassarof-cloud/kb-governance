package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record NeedResponse(
        Long id,
        String status,
        String taskStatus,
        OffsetDateTime lastDetectedAt,
        Long clusterId,
        String clusterSample,
        Long ruleId,
        String ruleName,
        String externalTicketId
) {
}
