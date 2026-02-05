package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record NeedResponse(
        Long id,
        String title,
        String system,
        long occurrences,
        OffsetDateTime lastOccurrenceAt,
        String status,
        String needType,
        String externalTicketId
) {
}
