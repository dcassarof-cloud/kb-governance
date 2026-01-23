package br.com.consisa.gov.kb.dto;

import java.time.Instant;

/**
 * Execução de sync (importação do Movidesk).
 */
public record SyncRunDto(
        Long id,
        String mode,
        Instant startedAt,
        Instant finishedAt,
        String status,
        String note
) {}
