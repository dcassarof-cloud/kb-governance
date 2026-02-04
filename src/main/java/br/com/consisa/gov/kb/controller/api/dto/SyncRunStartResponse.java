package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record SyncRunStartResponse(
        String runId,
        String mode,
        OffsetDateTime startedAt,
        String status,
        String message
) {
}
