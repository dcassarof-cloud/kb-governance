package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

public record GovernanceOverviewResponse(
        GovernanceOverviewTotalsDto totals,
        List<GovernanceOverviewSystemDto> systems,
        double healthScore
) {
}
