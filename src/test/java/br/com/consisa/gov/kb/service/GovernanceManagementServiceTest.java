package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.controller.api.dto.GovernanceManagementDashboardResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceWorkloadResponse;
import br.com.consisa.gov.kb.domain.DetectedNeed;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernancePriorityLevel;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.domain.RecurrenceRule;
import br.com.consisa.gov.kb.repository.DetectedNeedRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.repository.RecurrenceRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceManagementServiceTest {

    @Mock
    private KbGovernanceIssueRepository issueRepository;

    @Mock
    private DetectedNeedRepository needRepository;

    @Mock
    private RecurrenceRuleRepository ruleRepository;

    @Mock
    private GovernanceIssuePriorityService priorityService;

    @InjectMocks
    private GovernanceManagementService managementService;

    @Test
    void buildDashboardAggregatesMetrics() {
        when(issueRepository.countTotalIssues()).thenReturn(20L);
        when(issueRepository.countOpenIssues()).thenReturn(8L);
        when(issueRepository.countOverdueOpenIssues()).thenReturn(3L);
        when(issueRepository.countUnassignedOpenIssues()).thenReturn(2L);

        when(issueRepository.countByStatus()).thenReturn(List.of(
                new StatusCountRowStub(GovernanceIssueStatus.OPEN, 5L),
                new StatusCountRowStub(GovernanceIssueStatus.RESOLVED, 15L)
        ));

        when(issueRepository.countBySystem()).thenReturn(List.of(
                new SystemCountRowStub("CONSISANET", "ConsisaNET", 10L)
        ));

        when(issueRepository.countByIssueType()).thenReturn(List.of(
                new IssueTypeCountRowStub(KbGovernanceIssueType.DUPLICATE_CONTENT, 4L)
        ));

        DetectedNeed recurringNeed = new DetectedNeed();
        recurringNeed.setRuleId(1L);
        DetectedNeed operationalNeed = new DetectedNeed();
        operationalNeed.setRuleId(2L);
        when(needRepository.findAll()).thenReturn(List.of(recurringNeed, operationalNeed));
        when(needRepository.countByStatus("OPEN")).thenReturn(2L);

        RecurrenceRule recurringRule = new RecurrenceRule();
        ReflectionTestUtils.setField(recurringRule, "id", 1L);
        recurringRule.setThresholdCount(3);
        RecurrenceRule operationalRule = new RecurrenceRule();
        ReflectionTestUtils.setField(operationalRule, "id", 2L);
        operationalRule.setThresholdCount(1);
        when(ruleRepository.findAllById(anyIterable())).thenReturn(List.of(recurringRule, operationalRule));

        KbGovernanceIssueRepository.IssueRow issueRow = new IssueRowStub(
                101L,
                "DUPLICATE_CONTENT",
                "ERROR",
                "OPEN",
                "CONSISANET",
                "ConsisaNET",
                Instant.now()
        );
        when(issueRepository.listOpenIssueRows()).thenReturn(List.of(issueRow));
        when(priorityService.assess("ERROR", "DUPLICATE_CONTENT", "OPEN", any()))
                .thenReturn(new GovernanceIssuePriorityService.PriorityAssessment(90, GovernancePriorityLevel.CRITICAL));

        GovernanceManagementDashboardResponse response = managementService.buildDashboard();

        assertThat(response.totalIssues()).isEqualTo(20L);
        assertThat(response.openIssues()).isEqualTo(8L);
        assertThat(response.overdueIssues()).isEqualTo(3L);
        assertThat(response.unassignedIssues()).isEqualTo(2L);
        assertThat(response.needsOpen()).isEqualTo(2L);
        assertThat(response.needsRecurring()).isEqualTo(1L);
        assertThat(response.issuesByStatus()).hasSize(2);
        assertThat(response.issuesBySystem()).hasSize(1);
        assertThat(response.issuesByType()).hasSize(1);
        assertThat(response.topCriticalIssues()).hasSize(1);
    }

    @Test
    void buildWorkloadAggregatesPerResponsible() {
        when(issueRepository.fetchWorkloadRows()).thenReturn(List.of(
                new WorkloadRowStub("user-1", 4L, 1L, 7200.0, 2L)
        ));

        List<GovernanceWorkloadResponse> response = managementService.buildWorkload();

        assertThat(response).hasSize(1);
        GovernanceWorkloadResponse item = response.get(0);
        assertThat(item.responsibleId()).isEqualTo("user-1");
        assertThat(item.openIssues()).isEqualTo(4L);
        assertThat(item.overdueIssues()).isEqualTo(1L);
        assertThat(item.avgResolutionTimeHours()).isEqualTo(2.0);
        assertThat(item.systemsHandled()).isEqualTo(2L);
    }

    private record StatusCountRowStub(GovernanceIssueStatus status, Long total)
            implements KbGovernanceIssueRepository.StatusCountRow {
        @Override
        public GovernanceIssueStatus getStatus() {
            return status;
        }

        @Override
        public Long getTotal() {
            return total;
        }
    }

    private record SystemCountRowStub(String systemCode, String systemName, Long total)
            implements KbGovernanceIssueRepository.SystemCountRow {
        @Override
        public String getSystemCode() {
            return systemCode;
        }

        @Override
        public String getSystemName() {
            return systemName;
        }

        @Override
        public Long getTotal() {
            return total;
        }
    }

    private record IssueTypeCountRowStub(KbGovernanceIssueType issueType, Long total)
            implements KbGovernanceIssueRepository.IssueTypeCountRow {
        @Override
        public KbGovernanceIssueType getIssueType() {
            return issueType;
        }

        @Override
        public Long getTotal() {
            return total;
        }
    }

    private record IssueRowStub(
            Long id,
            String issueType,
            String severity,
            String status,
            String systemCode,
            String systemName,
            Instant slaDueAt
    ) implements KbGovernanceIssueRepository.IssueRow {
        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getIssueType() {
            return issueType;
        }

        @Override
        public String getSeverity() {
            return severity;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public Long getArticleId() {
            return null;
        }

        @Override
        public String getArticleTitle() {
            return null;
        }

        @Override
        public String getSystemCode() {
            return systemCode;
        }

        @Override
        public String getSystemName() {
            return systemName;
        }

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public Instant getCreatedAt() {
            return Instant.now();
        }

        @Override
        public Instant getUpdatedAt() {
            return null;
        }

        @Override
        public String getResponsibleId() {
            return null;
        }

        @Override
        public String getResponsibleType() {
            return null;
        }

        @Override
        public Instant getSlaDueAt() {
            return slaDueAt;
        }

        @Override
        public Instant getResolvedAt() {
            return null;
        }

        @Override
        public String getIgnoredReason() {
            return null;
        }

        @Override
        public String getAssignedAgentId() {
            return null;
        }

        @Override
        public String getAssignedAgentName() {
            return null;
        }

        @Override
        public Instant getDueDate() {
            return null;
        }
    }

    private record WorkloadRowStub(
            String responsibleId,
            Long openIssues,
            Long overdueIssues,
            Double avgResolutionSeconds,
            Long systemsHandled
    ) implements KbGovernanceIssueRepository.WorkloadRow {
        @Override
        public String getResponsibleId() {
            return responsibleId;
        }

        @Override
        public Long getOpenIssues() {
            return openIssues;
        }

        @Override
        public Long getOverdueIssues() {
            return overdueIssues;
        }

        @Override
        public Double getAvgResolutionSeconds() {
            return avgResolutionSeconds;
        }

        @Override
        public Long getSystemsHandled() {
            return systemsHandled;
        }
    }
}
