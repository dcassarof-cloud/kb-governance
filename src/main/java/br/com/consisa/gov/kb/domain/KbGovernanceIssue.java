package br.com.consisa.gov.kb.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Issue de governança (qualidade) gerada por detectores.
 * Ex: conteúdo incompleto, duplicado, desatualizado, inconsistente.
 */
@Entity
@Table(name = "kb_governance_issue")
public class KbGovernanceIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 50)
    private KbGovernanceIssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GovernanceIssueStatus status = GovernanceIssueStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private GovernanceSeverity severity = GovernanceSeverity.WARN;

    @Column(name = "message", length = 400)
    private String message;

    /**
     * Evidências em JSONB (Postgres).
     * Ex: {"textLen":123,"placeholder":true,"minChars":500}
     *
     * Mapeamento nativo do Hibernate (sem lib externa).
     */
    @Column(name = "evidence", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode evidence;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "ignored_reason", length = 400)
    private String ignoredReason;

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
        if (this.status == GovernanceIssueStatus.IGNORED) {
            if (this.ignoredReason == null || this.ignoredReason.isBlank()) {
                throw new IllegalStateException("ignored_reason é obrigatório quando status = IGNORED");
            }
        }
    }

    // ======================
    // Getters e Setters
    // ======================

    public Long getId() { return id; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public KbGovernanceIssueType getIssueType() { return issueType; }
    public void setIssueType(KbGovernanceIssueType issueType) { this.issueType = issueType; }

    public GovernanceIssueStatus getStatus() { return status; }
    public void setStatus(GovernanceIssueStatus status) { this.status = status; }

    public GovernanceSeverity getSeverity() { return severity; }
    public void setSeverity(GovernanceSeverity severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public JsonNode getEvidence() { return evidence; }
    public void setEvidence(JsonNode evidence) { this.evidence = evidence; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getIgnoredReason() { return ignoredReason; }
    public void setIgnoredReason(String ignoredReason) { this.ignoredReason = ignoredReason; }
}
