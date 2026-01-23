package br.com.consisa.gov.kb.dto;

import java.util.List;

/**
 * DTO do resumo do dashboard.
 * Mantém o contrato simples e estável para o front.
 */
public record DashboardSummaryDto(
        long totalArticles,
        long okCount,
        long issuesCount,
        long duplicatesCount,
        List<BySystem> bySystem,
        List<ByStatus> byStatus
) {
    public record BySystem(String systemCode, String systemName, long count) {}
    public record ByStatus(String status, long count) {}
}
