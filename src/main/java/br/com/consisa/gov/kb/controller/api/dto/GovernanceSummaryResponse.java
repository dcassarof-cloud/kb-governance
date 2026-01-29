package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

public record GovernanceSummaryResponse(
        long totalOpenIssues,
        List<RiskSummary> manualsByRisk,
        List<SlaSummary> slaOverdueByPriority,
        double iaReadyPercent,
        List<CriticalSystemSummary> topCriticalSystems
) {
    public record RiskSummary(String riskLevel, long total) {}

    public record SlaSummary(String priority, long total) {}

    public record CriticalSystemSummary(String systemCode, String systemName, long total) {}
}
