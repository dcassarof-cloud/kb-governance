package br.com.consisa.gov.kb.controller.api.dto;

import br.com.consisa.gov.kb.dto.GovernanceLabelDto;

import java.util.List;

public record DuplicateGroupDetailResponse(
        String id,
        String contentHash,
        GovernanceLabelDto status,
        List<DuplicateGroupArticleResponse> articles
) {
}
