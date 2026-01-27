package br.com.consisa.gov.kb.controller.api.dto;

/**
 * DTO para atualizar sync config.
 * 
 * Body esperado:
 * {
 *   "enabled": true,
 *   "mode": "DELTA_WINDOW",
 *   "intervalMinutes": 60,
 *   "daysBack": 2
 * }
 */
public record SyncConfigUpdateRequest(
        boolean enabled,
        String mode,
        int intervalMinutes,
        int daysBack
) {
}
