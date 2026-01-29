package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Estado atual de governança de um manual.
 */
@Entity
@Table(name = "kb_manual_task",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_kb_manual_task_article", columnNames = {"article_id"})
        }
)
public class KbManualTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KbManualTaskStatus status = KbManualTaskStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private KbManualRiskLevel riskLevel = KbManualRiskLevel.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private KbManualPriority priority = KbManualPriority.P3;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_type", length = 10)
    private KbManualAssigneeType assigneeType;

    @Column(name = "assignee_id", length = 100)
    private String assigneeId;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "ignored_reason", length = 300)
    private String ignoredReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        validateIgnoredReason();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        validateIgnoredReason();
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void validateIgnoredReason() {
        if (this.status == KbManualTaskStatus.IGNORED) {
            if (this.ignoredReason == null || this.ignoredReason.isBlank()) {
                throw new IllegalStateException("ignored_reason é obrigatório quando status = IGNORED");
            }
        }
    }

    public Long getId() { return id; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public KbManualTaskStatus getStatus() { return status; }
    public void setStatus(KbManualTaskStatus status) { this.status = status; }

    public KbManualRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(KbManualRiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public KbManualPriority getPriority() { return priority; }
    public void setPriority(KbManualPriority priority) { this.priority = priority; }

    public KbManualAssigneeType getAssigneeType() { return assigneeType; }
    public void setAssigneeType(KbManualAssigneeType assigneeType) { this.assigneeType = assigneeType; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public OffsetDateTime getDueAt() { return dueAt; }
    public void setDueAt(OffsetDateTime dueAt) { this.dueAt = dueAt; }

    public String getIgnoredReason() { return ignoredReason; }
    public void setIgnoredReason(String ignoredReason) { this.ignoredReason = ignoredReason; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
