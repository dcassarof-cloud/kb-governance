package br.com.consisa.gov.kb.controller.api.dto;

public record DuplicateGroupPrimaryRequest(
        Long primaryArticleId,
        String actor
) {
}
