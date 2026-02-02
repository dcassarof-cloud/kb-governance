package br.com.consisa.gov.kb.controller.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record GovernanceIssueAssignRequest(
        String agentId,
        String agentName,
        @Schema(type = "string", format = "date", example = "2026-02-09")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dueDate,
        String actor
) {
}
