package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Atribuição de um artigo a um agente para atualização
 */
@Entity
@Table(name = "kb_article_assignment")
public class KbArticleAssignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ========================================
    // REFERÊNCIAS
    // ========================================
    
    @Column(name = "article_id", nullable = false)
    private Long articleId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private KbAgent agent;
    
    // ========================================
    // TICKET DO MOVIDESK
    // ========================================
    
    @Column(name = "ticket_id", length = 50)
    private String ticketId;
    
    @Column(name = "ticket_url")
    private String ticketUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", nullable = false, length = 20)
    private TicketStatus ticketStatus = TicketStatus.NONE;

    @Column(name = "ticket_last_error", columnDefinition = "text")
    private String ticketLastError;

    @Column(name = "ticket_created_at")
    private OffsetDateTime ticketCreatedAt;

    @Column(name = "ticket_retry_count", nullable = false)
    private Integer ticketRetryCount = 0;
    
    // ========================================
    // DETALHES DA ATRIBUIÇÃO
    // ========================================
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssignmentReason reason;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentPriority priority = AssignmentPriority.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.PENDING;
    
    // ========================================
    // DATAS
    // ========================================
    
    @Column(name = "due_date")
    private OffsetDateTime dueDate;
    
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    
    @Column(name = "completion_note", length = 500)
    private String completionNote;
    
    // ========================================
    // METADATA
    // ========================================
    
    @Column(name = "assigned_by", length = 100)
    private String assignedBy;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    // ========================================
    // LIFECYCLE CALLBACKS
    // ========================================
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
    
    // ========================================
    // BUSINESS METHODS
    // ========================================
    
    /**
     * Verifica se a atribuição está atrasada
     */
    public boolean isOverdue() {
        if (status.isFinal() || dueDate == null) {
            return false;
        }
        return dueDate.isBefore(OffsetDateTime.now());
    }
    
    /**
     * Marca a atribuição como concluída
     */
    public void complete(String note) {
        this.status = AssignmentStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
        this.completionNote = note;
        
        // Atualiza estatísticas do agente
        if (this.agent != null) {
            this.agent.completeAssignment();
        }
    }
    
    /**
     * Marca a atribuição como cancelada
     */
    public void cancel(String reason) {
        this.status = AssignmentStatus.CANCELLED;
        this.completionNote = reason;
        
        // Atualiza estatísticas do agente
        if (this.agent != null) {
            this.agent.decrementAssigned();
        }
    }
    
    /**
     * Inicia a atribuição (muda para IN_PROGRESS)
     */
    public void start() {
        if (this.status == AssignmentStatus.PENDING) {
            this.status = AssignmentStatus.IN_PROGRESS;
        }
    }
    
    /**
     * Calcula quantos dias faltam para o prazo
     */
    public Long getDaysUntilDue() {
        if (dueDate == null) {
            return null;
        }
        long days = java.time.Duration.between(
            OffsetDateTime.now(),
            dueDate
        ).toDays();
        return days;
    }
    
    /**
     * Calcula quantos dias levou para concluir
     */
    public Long getCompletionDays() {
        if (completedAt == null) {
            return null;
        }
        return java.time.Duration.between(createdAt, completedAt).toDays();
    }
    
    // ========================================
    // GETTERS AND SETTERS
    // ========================================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getArticleId() {
        return articleId;
    }
    
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }
    
    public KbAgent getAgent() {
        return agent;
    }
    
    public void setAgent(KbAgent agent) {
        this.agent = agent;
    }
    
    public String getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
    
    public String getTicketUrl() {
        return ticketUrl;
    }
    
    public void setTicketUrl(String ticketUrl) {
        this.ticketUrl = ticketUrl;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public String getTicketLastError() {
        return ticketLastError;
    }

    public void setTicketLastError(String ticketLastError) {
        this.ticketLastError = ticketLastError;
    }

    public OffsetDateTime getTicketCreatedAt() {
        return ticketCreatedAt;
    }

    public void setTicketCreatedAt(OffsetDateTime ticketCreatedAt) {
        this.ticketCreatedAt = ticketCreatedAt;
    }

    public Integer getTicketRetryCount() {
        return ticketRetryCount;
    }

    public void setTicketRetryCount(Integer ticketRetryCount) {
        this.ticketRetryCount = ticketRetryCount;
    }
    
    public AssignmentReason getReason() {
        return reason;
    }
    
    public void setReason(AssignmentReason reason) {
        this.reason = reason;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public AssignmentPriority getPriority() {
        return priority;
    }
    
    public void setPriority(AssignmentPriority priority) {
        this.priority = priority;
    }
    
    public AssignmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }
    
    public OffsetDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getCompletionNote() {
        return completionNote;
    }
    
    public void setCompletionNote(String completionNote) {
        this.completionNote = completionNote;
    }
    
    public String getAssignedBy() {
        return assignedBy;
    }
    
    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
