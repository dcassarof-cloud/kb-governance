package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

public record DuplicateGroupDetailResponse(
        String id,
        String contentHash,
        String status,
        List<DuplicateGroupArticleResponse> articles
) {
}
