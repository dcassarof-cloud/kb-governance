package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

/**
 * Body esperado para revisão de manual.
 */
public record ManualTaskReviewRequest(
        List<String> checklist,
        Boolean approved,
        String actorType,
        String actorId,
        String actorName,
        String note
) {
}
