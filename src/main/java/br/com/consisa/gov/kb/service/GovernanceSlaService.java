package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.util.DateTimeUtils;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;

@Service
public class GovernanceSlaService {

    private static final Map<GovernanceSeverity, Integer> SLA_DAYS;

    static {
        Map<GovernanceSeverity, Integer> slaDays = new EnumMap<>(GovernanceSeverity.class);
        slaDays.put(GovernanceSeverity.ERROR, 3);
        slaDays.put(GovernanceSeverity.WARN, 15);
        slaDays.put(GovernanceSeverity.INFO, 30);
        SLA_DAYS = Map.copyOf(slaDays);
    }

    public OffsetDateTime calculateDueAt(OffsetDateTime baseDate, GovernanceSeverity severity) {
        if (baseDate == null || severity == null) {
            return null;
        }
        return baseDate.plusDays(getSlaDays(severity));
    }

    public OffsetDateTime calculateReopenedSlaDueAt(GovernanceSeverity severity) {
        if (severity == null) {
            return null;
        }
        return DateTimeUtils.nowSaoPaulo().plusDays(getSlaDays(severity));
    }

    public boolean isOverdue(OffsetDateTime now, OffsetDateTime slaDueAt, GovernanceIssueStatus status) {
        if (slaDueAt == null || status == null) {
            return false;
        }
        if (status == GovernanceIssueStatus.RESOLVED || status == GovernanceIssueStatus.IGNORED) {
            return false;
        }
        OffsetDateTime reference = now != null ? now : DateTimeUtils.nowSaoPaulo();
        return slaDueAt.isBefore(reference);
    }

    public int getSlaDays(GovernanceSeverity severity) {
        if (severity == null) {
            return 0;
        }
        return SLA_DAYS.getOrDefault(severity, 15);
    }
}
