package br.com.consisa.gov.kb.repository.projection;

import java.time.OffsetDateTime;

public record UnassignedIssue(
        long id,
        String issueType,
        String systemCode,
        String systemName,
        String severity,
        OffsetDateTime createdAt
) {
}
