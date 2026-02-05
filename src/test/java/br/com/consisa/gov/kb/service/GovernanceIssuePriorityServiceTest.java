package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernancePriorityLevel;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GovernanceIssuePriorityServiceTest {

    @Test
    void assessOverdueIssueReturnsCriticalScore() {
        GovernanceIssuePriorityService service = new GovernanceIssuePriorityService();
        OffsetDateTime dueAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);

        var assessment = service.assess("ERROR", "DUPLICATE_CONTENT", "OPEN", dueAt);

        int expectedScore = GovernanceIssuePriorityService.OVERDUE_WEIGHT
                + GovernanceIssuePriorityService.SEVERITY_ERROR_WEIGHT
                + GovernanceIssuePriorityService.TYPE_DUPLICATE_WEIGHT;
        assertThat(assessment.score()).isEqualTo(expectedScore);
        assertThat(assessment.level()).isEqualTo(GovernancePriorityLevel.CRITICAL);
    }

    @Test
    void assessDueSoonIssueReturnsMediumScore() {
        GovernanceIssuePriorityService service = new GovernanceIssuePriorityService();
        OffsetDateTime dueAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(10);

        var assessment = service.assess("WARN", "INCOMPLETE_CONTENT", "OPEN", dueAt);

        int expectedScore = GovernanceIssuePriorityService.DUE_SOON_WEIGHT
                + GovernanceIssuePriorityService.SEVERITY_WARN_WEIGHT
                + GovernanceIssuePriorityService.TYPE_INCOMPLETE_WEIGHT;
        assertThat(assessment.score()).isEqualTo(expectedScore);
        assertThat(assessment.level()).isEqualTo(GovernancePriorityLevel.MEDIUM);
    }
}
