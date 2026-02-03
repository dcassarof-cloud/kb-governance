package br.com.consisa.gov.kb.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "kb_article_ai_audit")
public class KbArticleAiAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false, unique = true)
    private Long articleId;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "missing_sections", columnDefinition = "text")
    private String missingSections;

    @Column(name = "details_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode detailsJson;

    @Column(name = "audited_at", nullable = false)
    private OffsetDateTime auditedAt;

    @PrePersist
    public void prePersist() {
        if (auditedAt == null) {
            auditedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getId() { return id; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getMissingSections() { return missingSections; }
    public void setMissingSections(String missingSections) { this.missingSections = missingSections; }

    public JsonNode getDetailsJson() { return detailsJson; }
    public void setDetailsJson(JsonNode detailsJson) { this.detailsJson = detailsJson; }

    public OffsetDateTime getAuditedAt() { return auditedAt; }
    public void setAuditedAt(OffsetDateTime auditedAt) { this.auditedAt = auditedAt; }
}
