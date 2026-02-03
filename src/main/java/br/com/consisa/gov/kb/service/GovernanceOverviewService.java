package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.controller.api.dto.GovernanceOverviewResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceOverviewSystemDto;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceOverviewTotalsDto;
import br.com.consisa.gov.kb.repository.GovernanceOverviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

@Service
public class GovernanceOverviewService {

    private final GovernanceOverviewRepository overviewRepository;

    public GovernanceOverviewService(GovernanceOverviewRepository overviewRepository) {
        this.overviewRepository = overviewRepository;
    }

    @Transactional(readOnly = true)
    public GovernanceOverviewResponse getOverview() {
        Object[] totalsRow = overviewRepository.fetchOverviewTotals();
        GovernanceOverviewTotalsDto totals = mapTotals(totalsRow);
        List<GovernanceOverviewSystemDto> systems = overviewRepository.fetchOverviewBySystem()
                .stream()
                .map(this::mapSystemRow)
                .toList();
        double healthScore = calculateHealthScore(totals.errorOpen(), totals.warnOpen(), totals.infoOpen());
        return new GovernanceOverviewResponse(totals, systems, healthScore);
    }

    private GovernanceOverviewTotalsDto mapTotals(Object[] row) {
        long totalOpen = toLong(row, 0);
        long errorOpen = toLong(row, 1);
        long warnOpen = toLong(row, 2);
        long infoOpen = toLong(row, 3);
        long overdueOpen = toLong(row, 4);
        long unassignedOpen = toLong(row, 5);
        return new GovernanceOverviewTotalsDto(
                totalOpen,
                errorOpen,
                warnOpen,
                infoOpen,
                overdueOpen,
                unassignedOpen,
                errorOpen
        );
    }

    private GovernanceOverviewSystemDto mapSystemRow(Object[] row) {
        String systemCode = row[0] != null ? row[0].toString() : null;
        String systemName = row[1] != null ? row[1].toString() : null;
        long totalOpen = toLong(row, 2);
        long errorOpen = toLong(row, 3);
        long warnOpen = toLong(row, 4);
        long infoOpen = toLong(row, 5);
        long overdueOpen = toLong(row, 6);
        long unassignedOpen = toLong(row, 7);
        double healthScore = calculateHealthScore(errorOpen, warnOpen, infoOpen);
        return new GovernanceOverviewSystemDto(
                systemCode,
                systemName,
                totalOpen,
                errorOpen,
                warnOpen,
                infoOpen,
                overdueOpen,
                unassignedOpen,
                errorOpen,
                healthScore
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
        return Long.parseLong(value.toString());
    }

    private double calculateHealthScore(long errorOpen, long warnOpen, long infoOpen) {
        double impact = (errorOpen * 5.0) + (warnOpen * 3.0) + (infoOpen * 1.0);
        return Math.max(0.0, 100.0 - impact);
    }
}
