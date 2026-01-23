package br.com.consisa.gov.kb.dto;

import java.time.Instant;

/**
 * Resposta de trigger do sync.
 */
public record SyncTriggerResponseDto(
        boolean triggered,
        Instant triggeredAt,
        String message
) {}
