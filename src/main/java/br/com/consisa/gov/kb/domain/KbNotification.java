package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Notificação in-app.
 */
@Entity
@Table(name = "kb_notification")
public class KbNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 10)
    private KbNotificationRecipientType recipientType;

    @Column(name = "recipient_id", nullable = false, length = 100)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private KbNotificationSeverity severity = KbNotificationSeverity.INFO;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (this.isRead == null) {
            this.isRead = Boolean.FALSE;
        }
    }

    public Long getId() { return id; }

    public KbNotificationRecipientType getRecipientType() { return recipientType; }
    public void setRecipientType(KbNotificationRecipientType recipientType) { this.recipientType = recipientType; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public KbNotificationSeverity getSeverity() { return severity; }
    public void setSeverity(KbNotificationSeverity severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
}
