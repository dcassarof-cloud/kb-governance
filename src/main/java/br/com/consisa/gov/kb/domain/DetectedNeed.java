package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "detected_need",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_detected_need_cluster_rule", columnNames = {"cluster_id", "rule_id"})
        }
)
public class DetectedNeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_id", nullable = false)
    private Long clusterId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "task_status", length = 30)
    private String taskStatus;

    @Column(name = "task_created_at")
    private OffsetDateTime taskCreatedAt;

    @Column(name = "external_ticket_id", length = 120)
    private String externalTicketId;

    @Column(name = "last_detected_at", nullable = false)
    private OffsetDateTime lastDetectedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }

    public Long getClusterId() { return clusterId; }
    public void setClusterId(Long clusterId) { this.clusterId = clusterId; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTaskStatus() { return taskStatus; }
    public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }

    public OffsetDateTime getTaskCreatedAt() { return taskCreatedAt; }
    public void setTaskCreatedAt(OffsetDateTime taskCreatedAt) { this.taskCreatedAt = taskCreatedAt; }

    public String getExternalTicketId() { return externalTicketId; }
    public void setExternalTicketId(String externalTicketId) { this.externalTicketId = externalTicketId; }

    public OffsetDateTime getLastDetectedAt() { return lastDetectedAt; }
    public void setLastDetectedAt(OffsetDateTime lastDetectedAt) { this.lastDetectedAt = lastDetectedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
