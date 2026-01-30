package br.com.consisa.gov.kb.dto;

import java.util.List;

/**
 * Resumo do Dashboard (contrato estável para o front).
 *
 * Dica Consisa 2026:
 * - Prefira DTOs enxutos para UI (não exponha Entity direto).
 */
public record DashboardSummaryDto(
        long totalArticles,
        long articlesOk,
        long articlesWithIssues,
        long totalIssues,
        long duplicatesCount,
        List<BySystem> bySystem,
        List<ByStatus> byStatus
) {
    public record BySystem(String systemCode, String systemName, long count) {}
    public record ByStatus(String status, long count) {}
}
