package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Histórico de ações em uma issue de governança.
 */
@Entity
@Table(name = "kb_governance_issue_history")
public class KbGovernanceIssueHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "old_value", length = 300)
    private String oldValue;

    @Column(name = "new_value", length = 300)
    private String newValue;

    @Column(name = "actor", length = 150)
    private String actor;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
