package br.com.consisa.gov.kb.controller.api.dto;

/**
 * Body esperado para mover manual de sistema/menu.
 */
public record ManualTaskMoveRequest(
        Long systemId,
        Long menuId,
        String menuName,
        String actorType,
        String actorId,
        String actorName,
        String note
) {
}
