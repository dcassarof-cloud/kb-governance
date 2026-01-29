package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

/**
 * Body esperado para atribuir responsável.
 */
public record ManualTaskAssignRequest(
        String assigneeType,
        String assigneeId,
        OffsetDateTime dueAt,
        String actorType,
        String actorId,
        String actorName,
        String note
) {
}
