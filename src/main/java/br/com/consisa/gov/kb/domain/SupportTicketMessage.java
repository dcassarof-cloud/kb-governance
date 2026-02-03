package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "support_ticket_message",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_support_ticket_message_key", columnNames = {"external_message_key"})
        }
)
public class SupportTicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "direction", nullable = false, length = 10)
    private String direction;

    @Column(name = "author", length = 150)
    private String author;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "content_html", columnDefinition = "text")
    private String contentHtml;

    @Column(name = "external_message_key", nullable = false, length = 200)
    private String externalMessageKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public String getExternalMessageKey() { return externalMessageKey; }
    public void setExternalMessageKey(String externalMessageKey) { this.externalMessageKey = externalMessageKey; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
