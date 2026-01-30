package br.com.consisa.gov.kb.controller.api.dto;

import java.util.List;

/**
 * DTO de resposta para o dashboard summary.
 * 
 * Formato esperado pelo front:
 * {
 *   "totalArticles": 1103,
 *   "articlesOk": 856,
 *   "articlesWithIssues": 247,
 *   "totalIssues": 412,
 *   "duplicatesCount": 23,
 *   "bySystem": [...],
 *   "byStatus": [...]
 * }
 */
public record DashboardSummaryResponse(
        long totalArticles,
        long articlesOk,
        long articlesWithIssues,
        long totalIssues,
        long duplicatesCount,
        List<BySystem> bySystem,
        List<ByStatus> byStatus
) {
    public record BySystem(
            String systemCode,
            String systemName,
            long count
    ) {}

    public record ByStatus(
            String status,
            long count
    ) {}
}
