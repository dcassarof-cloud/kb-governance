package br.com.consisa.gov.kb.controller.api.dto;

/**
 * DTO para sync config.
 * 
 * Formato esperado pelo front:
 * {
 *   "enabled": true,
 *   "mode": "DELTA_WINDOW",
 *   "intervalMinutes": 60,
 *   "daysBack": 2
 * }
 */
public record SyncConfigResponse(
        boolean enabled,
        br.com.consisa.gov.kb.dto.GovernanceLabelDto mode,
        int intervalMinutes,
        int daysBack
) {
}
