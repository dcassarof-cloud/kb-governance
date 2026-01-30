package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Assignment (responsável) de uma issue de governança.
 */
@Entity
@Table(name = "kb_governance_issue_assignment")
public class KbGovernanceIssueAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "agent_id", length = 100)
    private String agentId;

    @Column(name = "agent_name", length = 150)
    private String agentName;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GovernanceAssignmentStatus status = GovernanceAssignmentStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (this.assignedAt == null) {
            this.assignedAt = now;
        }
        this.createdAt = now;
    }

    public Long getId() { return id; }

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(OffsetDateTime assignedAt) { this.assignedAt = assignedAt; }

    public OffsetDateTime getDueDate() { return dueDate; }
    public void setDueDate(OffsetDateTime dueDate) { this.dueDate = dueDate; }

    public GovernanceAssignmentStatus getStatus() { return status; }
    public void setStatus(GovernanceAssignmentStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
