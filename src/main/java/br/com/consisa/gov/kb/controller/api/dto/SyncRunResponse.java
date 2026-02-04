package br.com.consisa.gov.kb.controller.api.dto;

import br.com.consisa.gov.kb.dto.GovernanceLabelDto;

import java.time.OffsetDateTime;

/**
 * DTO para sync run.
 * 
 * Formato esperado pelo front:
 * {
 *   "id": "run-123",
 *   "startedAt": "2024-01-19T10:30:00Z",
 *   "finishedAt": "2024-01-19T10:35:00Z",
 *   "status": "SUCCESS",
 *   "mode": "DELTA_WINDOW",
 *   "note": "Sync automático",
 *   "stats": {
 *     "articlesProcessed": 150,
 *     "articlesCreated": 10,
 *     "articlesUpdated": 140,
 *     "errors": 0
 *   }
 * }
 */
public record SyncRunResponse(
        String id,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        GovernanceLabelDto status,
        GovernanceLabelDto mode,
        String note,
        Stats stats
) {
    public record Stats(
            int articlesProcessed,
            int articlesCreated,
            int articlesUpdated,
            int errors
    ) {}
}
