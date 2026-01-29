package br.com.consisa.gov.kb.controller.api.dto;

/**
 * Body esperado para atualizar status.
 */
public record ManualTaskStatusRequest(
        String status,
        String ignoredReason,
        String actorType,
        String actorId,
        String actorName,
        String note
) {
}
