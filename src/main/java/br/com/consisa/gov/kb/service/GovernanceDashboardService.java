package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.controller.api.dto.GovernanceDashboardResponse;
import br.com.consisa.gov.kb.repository.GovernanceDashboardRepository;
import br.com.consisa.gov.kb.repository.GovernanceOverviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
public class GovernanceDashboardService {

    private static final int MAX_TOP_RISKS = 5;
    private static final int MAX_ATTENTION_ITEMS = 10;

    private final GovernanceOverviewRepository overviewRepository;
    private final GovernanceDashboardRepository dashboardRepository;
    private final GovernanceHealthScoreCalculator healthScoreCalculator;

    public GovernanceDashboardService(GovernanceOverviewRepository overviewRepository,
                                      GovernanceDashboardRepository dashboardRepository,
                                      GovernanceHealthScoreCalculator healthScoreCalculator) {
        this.overviewRepository = overviewRepository;
        this.dashboardRepository = dashboardRepository;
        this.healthScoreCalculator = healthScoreCalculator;
    }

    @Transactional(readOnly = true)
    public GovernanceDashboardResponse getGovernanceDashboard() {
        GovernanceDashboardResponse.Summary summary = buildSummary();
        List<GovernanceDashboardResponse.TopRisk> topRisks = buildTopRisks();
        GovernanceDashboardResponse.AttentionLists attentionLists = buildAttentionLists();
        GovernanceDashboardResponse.Trends trends = buildTrends();

        return new GovernanceDashboardResponse(
                summary,
                topRisks,
                attentionLists,
                trends,
                OffsetDateTime.now()
        );
    }

    private GovernanceDashboardResponse.Summary buildSummary() {
        Object[] totalsRow = overviewRepository.fetchOverviewTotals();
        long open = toLong(totalsRow, 0);
        long errorOpen = toLong(totalsRow, 1);
        long warnOpen = toLong(totalsRow, 2);
        long infoOpen = toLong(totalsRow, 3);
        long overdue = toLong(totalsRow, 4);
        long unassigned = toLong(totalsRow, 5);

        Object[] slaRow = dashboardRepository.fetchSlaComplianceTotals();
        long totalResolved = toLong(slaRow, 0);
        long resolvedOnTime = toLong(slaRow, 1);
        double slaCompliancePercent = totalResolved == 0
                ? 0.0
                : (resolvedOnTime * 100.0) / totalResolved;

        return new GovernanceDashboardResponse.Summary(
                open,
                errorOpen,
                warnOpen,
                infoOpen,
                overdue,
                unassigned,
                slaCompliancePercent
        );
    }

    private List<GovernanceDashboardResponse.TopRisk> buildTopRisks() {
        return overviewRepository.fetchOverviewBySystem()
                .stream()
                .map(row -> {
                    String systemCode = asString(row, 0);
                    String systemName = asString(row, 1);
                    long errorOpen = toLong(row, 3);
                    long warnOpen = toLong(row, 4);
                    long infoOpen = toLong(row, 5);
                    long overdue = toLong(row, 6);
                    long unassigned = toLong(row, 7);
                    double healthScore = healthScoreCalculator.calculate(errorOpen, warnOpen, infoOpen);
                    return new GovernanceDashboardResponse.TopRisk(
                            systemCode != null ? systemCode : "UNCLASSIFIED",
                            systemName != null ? systemName : "Não classificado",
                            healthScore,
                            errorOpen,
                            overdue,
                            unassigned
                    );
                })
                .sorted(Comparator.comparingDouble(GovernanceDashboardResponse.TopRisk::healthScore))
                .limit(MAX_TOP_RISKS)
                .toList();
    }

    private GovernanceDashboardResponse.AttentionLists buildAttentionLists() {
        List<GovernanceDashboardResponse.OverdueIssue> overdueIssues = dashboardRepository
                .fetchOverdueIssues(MAX_ATTENTION_ITEMS)
                .stream()
                .map(row -> new GovernanceDashboardResponse.OverdueIssue(
                        toLong(row, 0),
                        asString(row, 1),
                        asString(row, 2),
                        asString(row, 3),
                        asString(row, 4),
                        toOffsetDateTime(row[5]),
                        toOffsetDateTime(row[6])
                ))
                .toList();

        List<GovernanceDashboardResponse.UnassignedIssue> unassignedIssues = dashboardRepository
                .fetchUnassignedIssues(MAX_ATTENTION_ITEMS)
                .stream()
                .map(row -> new GovernanceDashboardResponse.UnassignedIssue(
                        toLong(row, 0),
                        asString(row, 1),
                        asString(row, 2),
                        asString(row, 3),
                        asString(row, 4),
                        toOffsetDateTime(row[5])
                ))
                .toList();

        return new GovernanceDashboardResponse.AttentionLists(overdueIssues, unassignedIssues);
    }

    private GovernanceDashboardResponse.Trends buildTrends() {
        Object[] row = dashboardRepository.fetchTrendsTotals();
        long opened = toLong(row, 0);
        long resolved = toLong(row, 1);
        long overdueLast7 = toLong(row, 2);
        long overduePrev7 = toLong(row, 3);
        long overdueVariation = overdueLast7 - overduePrev7;

        return new GovernanceDashboardResponse.Trends(
                new GovernanceDashboardResponse.Last7Days(opened, resolved, overdueVariation)
        );
    }

    private long toLong(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0L;
        }
        Object value = row[index];
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String asString(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return null;
        }
        return row[index].toString();
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime) {
            return (OffsetDateTime) value;
        }
        if (value instanceof java.time.Instant) {
            return OffsetDateTime.ofInstant((java.time.Instant) value, ZoneOffset.UTC);
        }
        if (value instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) value).atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(value.toString());
    }
}
