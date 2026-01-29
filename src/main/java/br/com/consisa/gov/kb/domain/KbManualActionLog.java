package br.com.consisa.gov.kb.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Auditoria de ações de governança operacional.
 */
@Entity
@Table(name = "kb_manual_action_log")
public class KbManualActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private KbManualActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10)
    private KbManualActorType actorType;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "payload_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payloadJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public KbManualActionType getActionType() { return actionType; }
    public void setActionType(KbManualActionType actionType) { this.actionType = actionType; }

    public KbManualActorType getActorType() { return actorType; }
    public void setActorType(KbManualActorType actorType) { this.actorType = actorType; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public JsonNode getPayloadJson() { return payloadJson; }
    public void setPayloadJson(JsonNode payloadJson) { this.payloadJson = payloadJson; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
