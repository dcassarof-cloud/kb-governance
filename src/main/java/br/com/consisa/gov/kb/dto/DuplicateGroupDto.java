package br.com.consisa.gov.kb.dto;

import java.util.List;

/**
 * Grupo de duplicados por hash (ex: content_hash).
 */
public record DuplicateGroupDto(
        String contentHash,
        int count,
        List<Long> articleIds
) {}
