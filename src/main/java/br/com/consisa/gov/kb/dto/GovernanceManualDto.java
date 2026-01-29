package br.com.consisa.gov.kb.dto;

import java.time.Instant;

/**
 * DTO de manual/artigo para tela de governança.
 */
public record GovernanceManualDto(
        Long id,
        String title,
        String systemCode,
        String systemName,
        String governanceStatus,
        Instant updatedAt,
        long issuesCount
) {}
