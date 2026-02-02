package br.com.consisa.gov.kb.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Grupo de duplicados por hash (ex: content_hash).
 */
public record DuplicateGroupDto(
        String hash,
        int count,
        List<DuplicateArticleDto> articles
) {
    public record DuplicateArticleDto(
            Long id,
            String title,
            String systemCode,
            String url,
            OffsetDateTime updatedAt
    ) {}
}
