package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record RecurringNeedItemResponse(
        String id,
        String subject,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime dueAt,
        String systemCode,
        String recurrenceKey,
        String link
) {
}
