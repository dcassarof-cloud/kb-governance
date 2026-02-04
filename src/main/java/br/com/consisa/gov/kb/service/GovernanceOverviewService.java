package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.controller.api.dto.GovernanceOverviewResponse;
import br.com.consisa.gov.kb.repository.GovernanceOverviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class GovernanceOverviewService {

    private final GovernanceOverviewRepository overviewRepository;
    private final GovernanceHealthScoreCalculator healthScoreCalculator;

    public GovernanceOverviewService(GovernanceOverviewRepository overviewRepository,
                                     GovernanceHealthScoreCalculator healthScoreCalculator) {
        this.overviewRepository = overviewRepository;
        this.healthScoreCalculator = healthScoreCalculator;
    }

    @Transactional(readOnly = true)
    public GovernanceOverviewResponse getOverview() {
        Object[] totalsRow = overviewRepository.fetchOverviewTotals();
        GovernanceOverviewResponse.Totals totals = mapTotals(totalsRow);
        List<GovernanceOverviewResponse.SystemOverview> systems = overviewRepository.fetchOverviewBySystem()
                .stream()
                .map(this::mapSystemRow)
                .toList();
        return new GovernanceOverviewResponse(totals, systems, OffsetDateTime.now());
    }

    private GovernanceOverviewResponse.Totals mapTotals(Object[] row) {
        long totalOpen = toLong(row, 0);
        long errorOpen = toLong(row, 1);
        long overdueOpen = toLong(row, 4);
        long unassignedOpen = toLong(row, 5);
        return new GovernanceOverviewResponse.Totals(totalOpen, errorOpen, unassignedOpen, overdueOpen);
    }

    private GovernanceOverviewResponse.SystemOverview mapSystemRow(Object[] row) {
        String systemCode = row[0] != null ? row[0].toString() : null;
        String systemName = row[1] != null ? row[1].toString() : null;
        long totalOpen = toLong(row, 2);
        long errorOpen = toLong(row, 3);
        long warnOpen = toLong(row, 4);
        long infoOpen = toLong(row, 5);
        long overdueOpen = toLong(row, 6);
        long unassignedOpen = toLong(row, 7);
        double healthScore = healthScoreCalculator.calculate(errorOpen, warnOpen, infoOpen);
        return new GovernanceOverviewResponse.SystemOverview(systemCode, systemName, totalOpen, errorOpen,
                overdueOpen, unassignedOpen, healthScore);
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
        return Long.parseLong(value.toString());
    }
}
