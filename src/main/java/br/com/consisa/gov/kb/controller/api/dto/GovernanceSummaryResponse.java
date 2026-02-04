package br.com.consisa.gov.kb.controller.api.dto;

import br.com.consisa.gov.kb.dto.GovernanceLabelDto;

import java.util.List;

public record GovernanceSummaryResponse(
        long totalArticles,
        long totalIssues,
        long articlesWithIssues,
        long articlesOk,
        long totalOpenIssues,
        List<IssueTypeSummary> issuesByType,
        List<RiskSummary> manualsByRisk,
        List<SlaSummary> slaOverdueByPriority,
        double iaReadyPercent,
        List<CriticalSystemSummary> topCriticalSystems
) {
    public record IssueTypeSummary(GovernanceLabelDto issueType, long total) {}

    public record RiskSummary(GovernanceLabelDto riskLevel, long total) {}

    public record SlaSummary(GovernanceLabelDto priority, long total) {}

    public record CriticalSystemSummary(String systemCode, String systemName, long total) {}
}
