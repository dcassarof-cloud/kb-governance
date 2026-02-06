package br.com.consisa.gov.kb.repository.projection;

import java.time.OffsetDateTime;

public record OverdueIssue(
        long id,
        String issueType,
        String systemCode,
        String systemName,
        String severity,
        OffsetDateTime slaDueAt,
        OffsetDateTime createdAt
) {
}
