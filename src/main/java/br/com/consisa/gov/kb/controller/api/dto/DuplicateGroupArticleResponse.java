package br.com.consisa.gov.kb.controller.api.dto;

import java.time.OffsetDateTime;

public record DuplicateGroupArticleResponse(
        Long id,
        String title,
        String system,
        OffsetDateTime updatedAt,
        String url
) {
}
