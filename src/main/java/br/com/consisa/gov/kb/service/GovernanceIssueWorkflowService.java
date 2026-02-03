package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernanceAssignmentStatus;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueAssignment;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueHistory;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueAssignmentRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueHistoryRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
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

    public GovernanceIssueWorkflowService(
            KbGovernanceIssueRepository issueRepository,
            KbGovernanceIssueAssignmentRepository assignmentRepository,
            KbGovernanceIssueHistoryRepository historyRepository
    ) {
        this.issueRepository = issueRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public KbGovernanceIssueAssignment assignIssue(
            Long issueId,
            String agentId,
            String agentName,
            LocalDate dueDate,
            String actor
    ) {
        KbGovernanceIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue não encontrada: " + issueId));

        GovernanceIssueStatus previousStatus = issue.getStatus();
        issue.setStatus(GovernanceIssueStatus.ASSIGNED);
        issue.setResolvedAt(null);
        issue.setResolvedBy(null);

        KbGovernanceIssueAssignment assignment = new KbGovernanceIssueAssignment();
        assignment.setIssueId(issueId);
        assignment.setAgentId(agentId);
        assignment.setAgentName(agentName);
        assignment.setAssignedAt(OffsetDateTime.now(ZoneOffset.UTC));
        OffsetDateTime dueDateTime = null;
        if (dueDate != null) {
            dueDateTime = dueDate.atStartOfDay().atOffset(ZoneOffset.ofHours(-3));
            assignment.setDueDate(dueDateTime);
        }
        assignment.setStatus(GovernanceAssignmentStatus.OPEN);
        assignmentRepository.save(assignment);

        issueRepository.save(issue);

        saveHistory(issueId, "ASSIGNED", previousStatus, issue.getStatus(), actor,
                buildAssignmentValue(agentId, agentName, dueDateTime));

        log.info("📌 Issue {} atribuída para {} ({})", issueId, agentName, agentId);
        return assignment;
    }

    @Transactional
    public KbGovernanceIssue updateStatus(Long issueId, GovernanceIssueStatus newStatus, String actor) {
        KbGovernanceIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue não encontrada: " + issueId));

        GovernanceIssueStatus previousStatus = issue.getStatus();
        if (previousStatus == newStatus) {
            return issue;
        }

        issue.setStatus(newStatus);
        if (newStatus != GovernanceIssueStatus.IGNORED) {
            issue.setIgnoredReason(null);
        }

        if (newStatus == GovernanceIssueStatus.RESOLVED || newStatus == GovernanceIssueStatus.IGNORED) {
            issue.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
            issue.setResolvedBy(actor);
        } else {
            issue.setResolvedAt(null);
            issue.setResolvedBy(null);
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

        saveHistory(issueId, "STATUS_CHANGED", previousStatus, newStatus, actor, null);

        log.info("📝 Issue {} status {} -> {}", issueId, previousStatus, newStatus);
        return issue;
    }

    @Transactional
    public KbGovernanceIssue ignoreIssue(Long issueId, String reason, String actor) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Motivo é obrigatório para ignorar uma issue");
        }
        KbGovernanceIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue não encontrada: " + issueId));

        GovernanceIssueStatus previousStatus = issue.getStatus();
        issue.setStatus(GovernanceIssueStatus.IGNORED);
        issue.setIgnoredReason(reason);
        issue.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
        issue.setResolvedBy(actor);

        assignmentRepository.findTop1ByIssueIdOrderByCreatedAtDesc(issueId)
                .ifPresent(assignment -> {
                    assignment.setStatus(GovernanceAssignmentStatus.DONE);
                    assignmentRepository.save(assignment);
                });

        issueRepository.save(issue);

        saveHistory(issueId, "IGNORED", previousStatus, GovernanceIssueStatus.IGNORED, actor, reason);

        log.info("🧾 Issue {} ignorada. Motivo: {}", issueId, reason);
        return issue;
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
                issue.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
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
}
