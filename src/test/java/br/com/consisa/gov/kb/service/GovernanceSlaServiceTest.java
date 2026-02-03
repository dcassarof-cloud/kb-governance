package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.util.DateTimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceSlaServiceTest {

    private final GovernanceSlaService slaService = new GovernanceSlaService();

    @Test
    void calculatesDueAtBySeverity() {
        OffsetDateTime base = OffsetDateTime.parse("2026-01-10T10:00:00-03:00");

        assertThat(slaService.calculateDueAt(base, GovernanceSeverity.ERROR))
                .isEqualTo(base.plusDays(3));
        assertThat(slaService.calculateDueAt(base, GovernanceSeverity.WARN))
                .isEqualTo(base.plusDays(15));
        assertThat(slaService.calculateDueAt(base, GovernanceSeverity.INFO))
                .isEqualTo(base.plusDays(30));
    }

    @Test
    void overdueRespectsStatusAndNullSla() {
        OffsetDateTime now = OffsetDateTime.parse("2026-02-01T10:00:00-03:00");
        OffsetDateTime due = now.minusDays(1);

        assertThat(slaService.isOverdue(now, due, GovernanceIssueStatus.OPEN)).isTrue();
        assertThat(slaService.isOverdue(now, due, GovernanceIssueStatus.RESOLVED)).isFalse();
        assertThat(slaService.isOverdue(now, null, GovernanceIssueStatus.OPEN)).isFalse();
    }

    @Test
    void reopeningRecalculatesSlaFromNow() {
        OffsetDateTime before = DateTimeUtils.nowSaoPaulo();
        OffsetDateTime dueAt = slaService.calculateReopenedSlaDueAt(GovernanceSeverity.WARN);
        OffsetDateTime after = DateTimeUtils.nowSaoPaulo();

        Duration diff = Duration.between(before, dueAt);
        assertThat(diff.toHours()).isBetween(15L * 24 - 1, 15L * 24 + 1);
        assertThat(dueAt).isAfter(before.minusSeconds(1));
        assertThat(dueAt).isBefore(after.plusDays(15).plusSeconds(5));
    }
}
