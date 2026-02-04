package br.com.consisa.gov.kb.controller.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record DuplicateGroupPrimaryRequest(
        @JsonAlias("articleId")
        Long primaryArticleId,
        String actor
) {
}
