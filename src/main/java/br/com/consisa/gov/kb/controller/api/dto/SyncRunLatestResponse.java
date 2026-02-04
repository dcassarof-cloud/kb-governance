package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record SyncRunLatestResponse(
        String runId,
        String mode,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String status,
        Stats stats
) {
    public record Stats(
            int processed,
            int created,
            int updated,
            int errors
    ) {
    }
}
