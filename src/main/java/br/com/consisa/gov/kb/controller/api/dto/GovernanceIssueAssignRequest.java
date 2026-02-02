package br.com.consisa.gov.kb.controller.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record GovernanceIssueAssignRequest(
        String agentId,
        String agentName,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dueDate,
        String actor
) {
}
