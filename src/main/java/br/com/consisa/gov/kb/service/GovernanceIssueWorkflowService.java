package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernanceAssignmentStatus;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceResponsibleType;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueAssignment;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueHistory;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueAssignmentRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueHistoryRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.util.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class GovernanceIssueWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(GovernanceIssueWorkflowService.class);

    private final KbGovernanceIssueRepository issueRepository;
    private final KbGovernanceIssueAssignmentRepository assignmentRepository;
    private final KbGovernanceIssueHistoryRepository historyRepository;
    private final GovernanceSlaService slaService;
    private final KbGovernanceIssueHistoryService richHistoryService;

    public GovernanceIssueWorkflowService(
            KbGovernanceIssueRepository issueRepository,
            KbGovernanceIssueAssignmentRepository assignmentRepository,
            KbGovernanceIssueHistoryRepository historyRepository,
            GovernanceSlaService slaService,
            KbGovernanceIssueHistoryService richHistoryService
    ) {
        this.issueRepository = issueRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.slaService = slaService;
        this.richHistoryService = richHistoryService;
    }

    @Transactional
    public KbGovernanceIssueAssignment assignIssue(
            Long issueId,
            String agentId,
            String agentName,
            LocalDate dueDate,
            String actor
    ) {
        return assignResponsible(issueId, GovernanceResponsibleType.USER, agentId, agentName, dueDate, actor);
    }

    @Transactional
    public KbGovernanceIssueAssignment assignResponsible(
            Long issueId,
            GovernanceResponsibleType responsibleType,
            String responsibleId,
            String responsibleName,
            LocalDate dueDate,
            String actor
    ) {
        KbGovernanceIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue não encontrada: " + issueId));

        GovernanceIssueStatus previousStatus = issue.getStatus();
        KbGovernanceIssue beforeChange = snapshot(issue);
        boolean wasAssigned = issue.getResponsibleId() != null;

        if (responsibleId == null || responsibleId.isBlank()) {
            issue.setResponsibleId(null);
            issue.setResponsibleType(null);
            if (issue.getStatus() == GovernanceIssueStatus.ASSIGNED) {
                issue.setStatus(GovernanceIssueStatus.OPEN);
            }
        } else {
            issue.setResponsibleId(responsibleId);
            issue.setResponsibleType(responsibleType);
            if (issue.getStatus() == GovernanceIssueStatus.RESOLVED || issue.getStatus() == GovernanceIssueStatus.IGNORED) {
                issue.setStatus(GovernanceIssueStatus.OPEN);
                issue.setResolvedAt(null);
                issue.setResolvedBy(null);
                issue.setIgnoredReason(null);
                issue.setSlaDueAt(slaService.calculateReopenedSlaDueAt(issue.getSeverity()));
            }
        }

        KbGovernanceIssueAssignment assignment = new KbGovernanceIssueAssignment();
        assignment.setIssueId(issueId);
        assignment.setAgentId(responsibleId);
        assignment.setAgentName(responsibleName);
        assignment.setAssignedAt(DateTimeUtils.nowSaoPaulo());
        OffsetDateTime dueDateTime = null;
        if (dueDate != null) {
            dueDateTime = dueDate.atStartOfDay().atOffset(ZoneOffset.ofHours(-3));
            assignment.setDueDate(dueDateTime);
        }
        assignment.setStatus(GovernanceAssignmentStatus.OPEN);
        if (responsibleId != null && !responsibleId.isBlank()) {
            assignmentRepository.save(assignment);
        }

        issueRepository.save(issue);

        if (responsibleId != null && !responsibleId.isBlank()) {
            richHistoryService.recordAssigned(beforeChange, issue, actor);
            saveHistory(issueId, "ASSIGNED", previousStatus, issue.getStatus(), actor,
                    buildAssignmentValue(responsibleId, responsibleName, dueDateTime));
        } else if (wasAssigned) {
            richHistoryService.recordUnassigned(beforeChange, issue, actor);
            saveHistory(issueId, "UNASSIGNED", previousStatus, issue.getStatus(), actor, null);
        }

        log.info("📌 Issue {} atribuída para {} ({})", issueId, responsibleName, responsibleId);
        return assignment;
    }

    @Transactional
    public KbGovernanceIssue updateStatus(Long issueId, GovernanceIssueStatus newStatus, String actor) {
        return updateStatus(issueId, newStatus, actor, null);
    }

    @Transactional
    public KbGovernanceIssue updateStatus(Long issueId, GovernanceIssueStatus newStatus, String actor, String ignoredReason) {
        KbGovernanceIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue não encontrada: " + issueId));

        GovernanceIssueStatus previousStatus = issue.getStatus();
        KbGovernanceIssue beforeChange = snapshot(issue);
        if (previousStatus == newStatus) {
            return issue;
        }

        issue.setStatus(newStatus);
        if (newStatus == GovernanceIssueStatus.IGNORED) {
            if (ignoredReason == null || ignoredReason.isBlank()) {
                throw new IllegalArgumentException("ignored_reason é obrigatório quando status = IGNORED");
            }
            issue.setIgnoredReason(ignoredReason);
        } else {
            issue.setIgnoredReason(null);
        }

        if (newStatus == GovernanceIssueStatus.RESOLVED || newStatus == GovernanceIssueStatus.IGNORED) {
            issue.setResolvedAt(DateTimeUtils.nowSaoPaulo());
            issue.setResolvedBy(actor);
        } else {
            issue.setResolvedAt(null);
            issue.setResolvedBy(null);
        }
        if (newStatus == GovernanceIssueStatus.OPEN
                && (previousStatus == GovernanceIssueStatus.RESOLVED || previousStatus == GovernanceIssueStatus.IGNORED)) {
            issue.setSlaDueAt(slaService.calculateReopenedSlaDueAt(issue.getSeverity()));
        }
        if (newStatus == GovernanceIssueStatus.ASSIGNED && issue.getResponsibleId() == null) {
            throw new IllegalStateException("responsible_id é obrigatório quando status = ASSIGNED");
        }

        assignmentRepository.findTop1ByIssueIdOrderByCreatedAtDesc(issueId)
                .ifPresent(assignment -> {
                    if (newStatus == GovernanceIssueStatus.ASSIGNED) {
                        assignment.setStatus(GovernanceAssignmentStatus.OPEN);
                        assignmentRepository.save(assignment);
                    } else if (newStatus == GovernanceIssueStatus.IN_PROGRESS) {
                        assignment.setStatus(GovernanceAssignmentStatus.IN_PROGRESS);
                        assignmentRepository.save(assignment);
                    } else if (newStatus == GovernanceIssueStatus.RESOLVED || newStatus == GovernanceIssueStatus.IGNORED) {
                        assignment.setStatus(GovernanceAssignmentStatus.DONE);
                        assignmentRepository.save(assignment);
                    }
                });

        issueRepository.save(issue);

        richHistoryService.recordStatusChanged(beforeChange, issue, actor);
        if (newStatus == GovernanceIssueStatus.OPEN
                && (previousStatus == GovernanceIssueStatus.RESOLVED || previousStatus == GovernanceIssueStatus.IGNORED)) {
            richHistoryService.recordReopened(beforeChange, issue, actor);
        }
        saveHistory(issueId, "STATUS_CHANGED", previousStatus, newStatus, actor, null);

        log.info("📝 Issue {} status {} -> {}", issueId, previousStatus, newStatus);
        return issue;
    }

    @Transactional
    public KbGovernanceIssue ignoreIssue(Long issueId, String reason, String actor) {
        return updateStatus(issueId, GovernanceIssueStatus.IGNORED, actor, reason);
    }

    @Transactional
    public void bulkUpdateStatus(List<Long> issueIds, GovernanceIssueStatus newStatus, String actor, String action, String newValue) {
        for (Long issueId : issueIds) {
            KbGovernanceIssue issue = issueRepository.findById(issueId)
                    .orElseThrow(() -> new IllegalArgumentException("Issue não encontrada: " + issueId));
            GovernanceIssueStatus previousStatus = issue.getStatus();
            if (previousStatus == newStatus) {
                continue;
            }

            issue.setStatus(newStatus);
            if (newStatus == GovernanceIssueStatus.RESOLVED || newStatus == GovernanceIssueStatus.IGNORED) {
                issue.setResolvedAt(DateTimeUtils.nowSaoPaulo());
                issue.setResolvedBy(actor);
                assignmentRepository.findTop1ByIssueIdOrderByCreatedAtDesc(issueId)
                        .ifPresent(assignment -> {
                            assignment.setStatus(GovernanceAssignmentStatus.DONE);
                            assignmentRepository.save(assignment);
                        });
            } else {
                issue.setResolvedAt(null);
                issue.setResolvedBy(null);
            }
            if (newStatus == GovernanceIssueStatus.OPEN
                    && (previousStatus == GovernanceIssueStatus.RESOLVED || previousStatus == GovernanceIssueStatus.IGNORED)) {
                issue.setSlaDueAt(slaService.calculateReopenedSlaDueAt(issue.getSeverity()));
            }
            issueRepository.save(issue);

            saveHistory(issueId, action, previousStatus, newStatus, actor, newValue);
        }
    }

    @Transactional(readOnly = true)
    public List<KbGovernanceIssueHistory> getHistory(Long issueId) {
        return historyRepository.findByIssueIdOrderByCreatedAtAsc(issueId);
    }

    @Transactional
    public void updateStatusIfOpen(Long articleId, KbGovernanceIssueType issueType, GovernanceIssueStatus newStatus, String actor) {
        issueRepository.findTop1ByArticleIdAndIssueTypeOrderByCreatedAtDesc(articleId, issueType)
                .filter(issue -> issue.getStatus() != GovernanceIssueStatus.RESOLVED && issue.getStatus() != GovernanceIssueStatus.IGNORED)
                .ifPresent(issue -> updateStatus(issue.getId(), newStatus, actor));
    }

    private void saveHistory(Long issueId,
                             String action,
                             GovernanceIssueStatus oldStatus,
                             GovernanceIssueStatus newStatus,
                             String actor,
                             String newValue) {
        KbGovernanceIssueHistory history = new KbGovernanceIssueHistory();
        history.setIssueId(issueId);
        history.setAction(action);
        history.setOldValue(oldStatus != null ? oldStatus.name() : null);
        history.setNewValue(newValue != null ? newValue : (newStatus != null ? newStatus.name() : null));
        history.setActor(actor);
        historyRepository.save(history);
    }

    private String buildAssignmentValue(String agentId, String agentName, OffsetDateTime dueDate) {
        StringBuilder sb = new StringBuilder();
        if (agentName != null && !agentName.isBlank()) {
            sb.append(agentName);
        }
        if (agentId != null && !agentId.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(agentId);
        }
        if (dueDate != null) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("due=").append(dueDate);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private KbGovernanceIssue snapshot(KbGovernanceIssue issue) {
        if (issue == null) {
            return null;
        }
        KbGovernanceIssue copy = new KbGovernanceIssue();
        copy.setStatus(issue.getStatus());
        copy.setSlaDueAt(issue.getSlaDueAt());
        copy.setResponsibleId(issue.getResponsibleId());
        copy.setResponsibleType(issue.getResponsibleType());
        return copy;
    }
}
