package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Agente do Movidesk que pode receber atribuições de atualização de artigos
 */
@Entity
@Table(name = "kb_agent")
public class KbAgent {
    
    @Id
    @Column(length = 50)
    private String id;  // ID do Movidesk (string numérica)
    
    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;
    
    @Column(name = "user_name", nullable = false, unique = true, length = 100)
    private String userName;
    
    @Column(length = 200)
    private String email;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    // ========================================
    // ESTATÍSTICAS DE PRODUTIVIDADE
    // ========================================
    
    /**
     * Quantidade de atribuições ativas (PENDING ou IN_PROGRESS)
     * Usado para balanceamento de carga
     */
    @Column(name = "assigned_count", nullable = false)
    private Integer assignedCount = 0;
    
    /**
     * Total de atribuições concluídas
     * Indica produtividade histórica
     */
    @Column(name = "completed_count", nullable = false)
    private Integer completedCount = 0;
    
    /**
     * Tempo médio em dias para concluir atribuições
     */
    @Column(name = "avg_completion_days")
    private Double avgCompletionDays;
    
    // ========================================
    // RELACIONAMENTOS
    // ========================================
    
    /**
     * Times/equipes do agente (ex: "ERP - EMPRESARIAL", "Team Leader")
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "kb_agent_team",
        joinColumns = @JoinColumn(name = "agent_id")
    )
    @Column(name = "team_name", length = 100)
    private Set<String> teams = new HashSet<>();
    
    /**
     * Especialidades/sistemas do agente (ex: "CONSISANET", "NOTAON", "SGRH")
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "kb_agent_specialty",
        joinColumns = @JoinColumn(name = "agent_id")
    )
    @Column(name = "system_code", length = 50)
    private Set<String> specialties = new HashSet<>();
    
    // ========================================
    // CONTROLE
    // ========================================
    
    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;
    
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
     * Verifica se o agente tem especialidade em um sistema específico
     */
    public boolean hasSpecialty(String systemCode) {
        return specialties != null && specialties.contains(systemCode);
    }
    
    /**
     * Verifica se o agente pertence a um time específico
     */
    public boolean isInTeam(String teamName) {
        return teams != null && teams.contains(teamName);
    }
    
    /**
     * Calcula taxa de sucesso (% de concluídas sobre total)
     */
    public Double getSuccessRate() {
        int total = completedCount + assignedCount;
        if (total == 0) return 0.0;
        return (completedCount.doubleValue() / total) * 100;
    }
    
    /**
     * Incrementa contador de atribuições
     */
    public void incrementAssigned() {
        this.assignedCount++;
    }
    
    /**
     * Decrementa atribuições e incrementa concluídas
     */
    public void completeAssignment() {
        if (this.assignedCount > 0) {
            this.assignedCount--;
        }
        this.completedCount++;
    }
    
    /**
     * Apenas decrementa atribuições (para cancelamento)
     */
    public void decrementAssigned() {
        if (this.assignedCount > 0) {
            this.assignedCount--;
        }
    }
    
    // ========================================
    // GETTERS AND SETTERS
    // ========================================
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getBusinessName() {
        return businessName;
    }
    
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Integer getAssignedCount() {
        return assignedCount;
    }
    
    public void setAssignedCount(Integer assignedCount) {
        this.assignedCount = assignedCount;
    }
    
    public Integer getCompletedCount() {
        return completedCount;
    }
    
    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }
    
    public Double getAvgCompletionDays() {
        return avgCompletionDays;
    }
    
    public void setAvgCompletionDays(Double avgCompletionDays) {
        this.avgCompletionDays = avgCompletionDays;
    }
    
    public Set<String> getTeams() {
        return teams;
    }
    
    public void setTeams(Set<String> teams) {
        this.teams = teams;
    }
    
    public Set<String> getSpecialties() {
        return specialties;
    }
    
    public void setSpecialties(Set<String> specialties) {
        this.specialties = specialties;
    }
    
    public OffsetDateTime getSyncedAt() {
        return syncedAt;
    }
    
    public void setSyncedAt(OffsetDateTime syncedAt) {
        this.syncedAt = syncedAt;
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
