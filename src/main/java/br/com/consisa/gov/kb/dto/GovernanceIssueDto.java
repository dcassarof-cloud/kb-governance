package br.com.consisa.gov.kb.dto;

import java.time.Instant;

/**
 * Issue de governança (ex: duplicado, curto demais, sem estrutura, etc).
 */
public record GovernanceIssueDto(
        Long id,
        String issueType,
        String severity,
        String status,
        Long articleId,
        String articleTitle,
        String systemCode,
        String systemName,
        String details,
        Instant createdAt
) {}
