package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;
import java.util.Map;

public record GovernanceSummaryResponse(
        long totalArticles,
        long totalIssues,
        long articlesWithIssues,
        long articlesOk,
        long totalOpenIssues,
        Map<String, Long> issuesByType,
        List<RiskSummary> manualsByRisk,
        List<SlaSummary> slaOverdueByPriority,
        double iaReadyPercent,
        List<CriticalSystemSummary> topCriticalSystems
) {
    public record RiskSummary(String riskLevel, long total) {}

    public record SlaSummary(String priority, long total) {}

    public record CriticalSystemSummary(String systemCode, String systemName, long total) {}
}
