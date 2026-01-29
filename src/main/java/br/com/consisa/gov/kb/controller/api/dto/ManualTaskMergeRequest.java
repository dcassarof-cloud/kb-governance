package br.com.consisa.gov.kb.controller.api.dto;

/**
 * Body esperado para mesclar manuais duplicados.
 */
public record ManualTaskMergeRequest(
        Long mergedIntoArticleId,
        String actorType,
        String actorId,
        String actorName,
        String note
) {
}
