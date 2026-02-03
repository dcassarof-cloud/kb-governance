package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "support_ticket",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_support_ticket_external", columnNames = {"external_ticket_id"})
        }
)
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_ticket_id", nullable = false, length = 120)
    private String externalTicketId;

    @Column(name = "protocol", length = 120)
    private String protocol;

    @Column(name = "subject", length = 300)
    private String subject;

    @Column(name = "status", length = 60)
    private String status;

    @Column(name = "requester", length = 150)
    private String requester;

    @Column(name = "owner_team", length = 150)
    private String ownerTeam;

    @Column(name = "origin_created_at")
    private OffsetDateTime originCreatedAt;

    @Column(name = "origin_updated_at")
    private OffsetDateTime originUpdatedAt;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

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

    public String getExternalTicketId() { return externalTicketId; }
    public void setExternalTicketId(String externalTicketId) { this.externalTicketId = externalTicketId; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequester() { return requester; }
    public void setRequester(String requester) { this.requester = requester; }

    public String getOwnerTeam() { return ownerTeam; }
    public void setOwnerTeam(String ownerTeam) { this.ownerTeam = ownerTeam; }

    public OffsetDateTime getOriginCreatedAt() { return originCreatedAt; }
    public void setOriginCreatedAt(OffsetDateTime originCreatedAt) { this.originCreatedAt = originCreatedAt; }

    public OffsetDateTime getOriginUpdatedAt() { return originUpdatedAt; }
    public void setOriginUpdatedAt(OffsetDateTime originUpdatedAt) { this.originUpdatedAt = originUpdatedAt; }

    public OffsetDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(OffsetDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
