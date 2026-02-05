package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.controller.api.dto.GovernanceManagementDashboardResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceWorkloadResponse;
import br.com.consisa.gov.kb.domain.DetectedNeed;
import br.com.consisa.gov.kb.domain.GovernancePriorityLevel;
import br.com.consisa.gov.kb.domain.RecurrenceRule;
import br.com.consisa.gov.kb.repository.DetectedNeedRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.repository.RecurrenceRuleRepository;
import br.com.consisa.gov.kb.util.DateTimeUtils;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GovernanceManagementService {

    private static final String NEED_STATUS_OPEN = "OPEN";

    private final KbGovernanceIssueRepository issueRepository;
    private final DetectedNeedRepository needRepository;
    private final RecurrenceRuleRepository ruleRepository;
    private final GovernanceIssuePriorityService priorityService;

    public GovernanceManagementService(
            KbGovernanceIssueRepository issueRepository,
            DetectedNeedRepository needRepository,
            RecurrenceRuleRepository ruleRepository,
            GovernanceIssuePriorityService priorityService
    ) {
        this.issueRepository = issueRepository;
        this.needRepository = needRepository;
        this.ruleRepository = ruleRepository;
        this.priorityService = priorityService;
    }

    public GovernanceManagementDashboardResponse buildDashboard() {
        long totalIssues = issueRepository.countTotalIssues();
        long openIssues = issueRepository.countOpenIssues();
        long overdueIssues = issueRepository.countOverdueOpenIssues();
        long unassignedIssues = issueRepository.countUnassignedOpenIssues();

        List<GovernanceManagementDashboardResponse.StatusCount> byStatus = issueRepository.countByStatus().stream()
                .map(row -> new GovernanceManagementDashboardResponse.StatusCount(
                        row.getStatus().name(),
                        row.getTotal() != null ? row.getTotal() : 0
                ))
                .toList();

        List<GovernanceManagementDashboardResponse.SystemCount> bySystem = issueRepository.countBySystem().stream()
                .map(row -> new GovernanceManagementDashboardResponse.SystemCount(
                        row.getSystemCode(),
                        row.getSystemName(),
                        row.getTotal() != null ? row.getTotal() : 0
                ))
                .toList();

        List<GovernanceManagementDashboardResponse.TypeCount> byType = issueRepository.countByIssueType().stream()
                .map(row -> new GovernanceManagementDashboardResponse.TypeCount(
                        row.getIssueType().name(),
                        row.getTotal() != null ? row.getTotal() : 0
                ))
                .toList();

        long needsOpen = needRepository.countByStatus(NEED_STATUS_OPEN);
        long needsRecurring = countRecurringNeeds();

        List<GovernanceManagementDashboardResponse.TopPriorityIssue> topCritical = issueRepository.listOpenIssueRows()
                .stream()
                .map(row -> {
                    OffsetDateTime slaDueAt = DateTimeUtils.toOffsetDateTimeOrNull(row.getSlaDueAt());
                    var assessment = priorityService.assess(row.getSeverity(), row.getIssueType(), row.getStatus(), slaDueAt);
                    boolean overdue = isOverdue(row.getStatus(), slaDueAt);
                    return new GovernanceManagementDashboardResponse.TopPriorityIssue(
                            row.getId(),
                            row.getIssueType(),
                            row.getSeverity(),
                            row.getStatus(),
                            row.getSystemCode(),
                            row.getSystemName(),
                            slaDueAt,
                            overdue,
                            assessment.score(),
                            assessment.level().name()
                    );
                })
                .filter(item -> GovernancePriorityLevel.CRITICAL.name().equals(item.priorityLevel()))
                .sorted(Comparator.comparingInt(GovernanceManagementDashboardResponse.TopPriorityIssue::priorityScore).reversed())
                .limit(10)
                .toList();

        return new GovernanceManagementDashboardResponse(
                totalIssues,
                openIssues,
                overdueIssues,
                unassignedIssues,
                byStatus,
                bySystem,
                byType,
                needsOpen,
                needsRecurring,
                topCritical
        );
    }

    public List<GovernanceWorkloadResponse> buildWorkload() {
        return issueRepository.fetchWorkloadRows().stream()
                .map(row -> new GovernanceWorkloadResponse(
                        row.getResponsibleId(),
                        row.getOpenIssues() != null ? row.getOpenIssues() : 0,
                        row.getOverdueIssues() != null ? row.getOverdueIssues() : 0,
                        toHours(row.getAvgResolutionSeconds()),
                        row.getSystemsHandled() != null ? row.getSystemsHandled() : 0
                ))
                .toList();
    }

    private long countRecurringNeeds() {
        List<DetectedNeed> needs = needRepository.findAll();
        if (needs.isEmpty()) {
            return 0;
        }
        List<Long> ruleIds = needs.stream()
                .map(DetectedNeed::getRuleId)
                .distinct()
                .toList();
        Map<Long, RecurrenceRule> rulesById = ruleRepository.findAllById(ruleIds).stream()
                .collect(Collectors.toMap(RecurrenceRule::getId, Function.identity()));
        return needs.stream()
                .filter(need -> {
                    RecurrenceRule rule = rulesById.get(need.getRuleId());
                    return rule != null && rule.getThresholdCount() > 1;
                })
                .count();
    }

    private boolean isOverdue(String status, OffsetDateTime slaDueAt) {
        if (slaDueAt == null || status == null) {
            return false;
        }
        String normalized = status.toUpperCase();
        if ("RESOLVED".equals(normalized) || "IGNORED".equals(normalized)) {
            return false;
        }
        return slaDueAt.isBefore(OffsetDateTime.now());
    }

    private double toHours(Double avgSeconds) {
        if (avgSeconds == null) {
            return 0;
        }
        return avgSeconds / 3600.0;
    }
}
