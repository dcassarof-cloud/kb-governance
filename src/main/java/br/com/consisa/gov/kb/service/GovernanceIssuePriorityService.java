package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernancePriorityLevel;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class GovernanceIssuePriorityService {

    public static final int OVERDUE_WEIGHT = 50;
    public static final int DUE_SOON_WEIGHT = 20;

    public static final int SEVERITY_ERROR_WEIGHT = 30;
    public static final int SEVERITY_WARN_WEIGHT = 15;
    public static final int SEVERITY_INFO_WEIGHT = 5;

    public static final int TYPE_DUPLICATE_WEIGHT = 18;
    public static final int TYPE_OUTDATED_WEIGHT = 16;
    public static final int TYPE_INCONSISTENT_WEIGHT = 14;
    public static final int TYPE_INCOMPLETE_WEIGHT = 12;
    public static final int TYPE_REVIEW_REQUIRED_WEIGHT = 10;
    public static final int TYPE_NOT_AI_READY_WEIGHT = 8;

    public static final int LEVEL_CRITICAL_THRESHOLD = 80;
    public static final int LEVEL_HIGH_THRESHOLD = 60;
    public static final int LEVEL_MEDIUM_THRESHOLD = 40;

    public PriorityAssessment assessIssue(KbGovernanceIssue issue) {
        if (issue == null) {
            return new PriorityAssessment(0, GovernancePriorityLevel.LOW);
        }
        return assess(
                issue.getSeverity() != null ? issue.getSeverity().name() : null,
                issue.getIssueType() != null ? issue.getIssueType().name() : null,
                issue.getStatus() != null ? issue.getStatus().name() : null,
                issue.getSlaDueAt()
        );
    }

    public PriorityAssessment assess(String severity, String issueType, String status, OffsetDateTime slaDueAt) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        boolean overdue = isOverdue(status, slaDueAt, now);
        boolean dueSoon = isDueSoon(slaDueAt, now);

        int score = 0;
        if (overdue) {
            score += OVERDUE_WEIGHT;
        } else if (dueSoon) {
            score += DUE_SOON_WEIGHT;
        }

        score += severityWeight(severity);
        score += issueTypeWeight(issueType);

        GovernancePriorityLevel level = resolveLevel(score);
        return new PriorityAssessment(score, level);
    }

    private boolean isOverdue(String status, OffsetDateTime slaDueAt, OffsetDateTime now) {
        if (slaDueAt == null || status == null) {
            return false;
        }
        String normalized = status.toUpperCase();
        if ("RESOLVED".equals(normalized) || "IGNORED".equals(normalized)) {
            return false;
        }
        return slaDueAt.isBefore(now);
    }

    private boolean isDueSoon(OffsetDateTime slaDueAt, OffsetDateTime now) {
        if (slaDueAt == null) {
            return false;
        }
        if (slaDueAt.isBefore(now)) {
            return false;
        }
        long daysToDue = ChronoUnit.DAYS.between(now.toLocalDate(), slaDueAt.toLocalDate());
        return daysToDue <= 1;
    }

    private int severityWeight(String severity) {
        if (severity == null) {
            return 0;
        }
        try {
            GovernanceSeverity parsed = GovernanceSeverity.valueOf(severity.toUpperCase());
            return switch (parsed) {
                case ERROR -> SEVERITY_ERROR_WEIGHT;
                case WARN -> SEVERITY_WARN_WEIGHT;
                case INFO -> SEVERITY_INFO_WEIGHT;
            };
        } catch (IllegalArgumentException ex) {
            return 0;
        }
    }

    private int issueTypeWeight(String issueType) {
        if (issueType == null) {
            return 0;
        }
        try {
            KbGovernanceIssueType parsed = KbGovernanceIssueType.valueOf(issueType.toUpperCase());
            return switch (parsed) {
                case DUPLICATE_CONTENT -> TYPE_DUPLICATE_WEIGHT;
                case OUTDATED_CONTENT -> TYPE_OUTDATED_WEIGHT;
                case INCONSISTENT_CONTENT -> TYPE_INCONSISTENT_WEIGHT;
                case INCOMPLETE_CONTENT -> TYPE_INCOMPLETE_WEIGHT;
                case REVIEW_REQUIRED -> TYPE_REVIEW_REQUIRED_WEIGHT;
                case NOT_AI_READY -> TYPE_NOT_AI_READY_WEIGHT;
            };
        } catch (IllegalArgumentException ex) {
            return 0;
        }
    }

    private GovernancePriorityLevel resolveLevel(int score) {
        if (score >= LEVEL_CRITICAL_THRESHOLD) {
            return GovernancePriorityLevel.CRITICAL;
        }
        if (score >= LEVEL_HIGH_THRESHOLD) {
            return GovernancePriorityLevel.HIGH;
        }
        if (score >= LEVEL_MEDIUM_THRESHOLD) {
            return GovernancePriorityLevel.MEDIUM;
        }
        return GovernancePriorityLevel.LOW;
    }

    public record PriorityAssessment(int score, GovernancePriorityLevel level) {
    }
}
