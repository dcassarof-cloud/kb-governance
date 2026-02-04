package br.com.consisa.gov.kb.controller.api.dto;

/**
 * DTO para disparar sync manual.
 * 
 * Body esperado (todos campos opcionais):
 * {
 *   "mode": "DELTA",
 *   "daysBack": 2,
 *   "note": "Sync manual disparado pelo usuário"
 * }
 */
public record TriggerSyncRequest(
        String mode,
        Integer daysBack,
        String note
) {
}
