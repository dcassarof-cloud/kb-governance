package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 📸 SNAPSHOT DIÁRIO DE MÉTRICAS DE GOVERNANÇA
 *
 * OBJETIVO:
 * ---------
 * Capturar estado das métricas em um ponto no tempo para:
 * - Análise de tendências
 * - Comparação de períodos
 * - Geração de gráficos históricos
 * - Relatórios executivos
 *
 * GRANULARIDADE:
 * --------------
 * - Global (system_code = null): métricas de todos os artigos
 * - Por Sistema (system_code != null): métricas de um sistema específico
 *
 * QUANDO É GERADO:
 * ----------------
 * - Automaticamente pelo scheduler diário (2h da manhã)
 * - Semanalmente (domingos) para análise mais profunda
 * - Sob demanda via API
 */
@Entity
@Table(name = "kb_governance_snapshot",
        uniqueConstraints = @UniqueConstraint(columnNames = {"snapshot_date", "system_code"}))
public class KbGovernanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Data do snapshot (sem hora, para facilitar agrupamentos)
     */
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    /**
     * Escopo do snapshot:
     * - null = métricas globais (todos os sistemas)
     * - "CONSISANET" = métricas apenas do ConsisaNET
     * - "NOTAON" = métricas apenas do NotaON
     * etc.
     */
    @Column(name = "system_code", length = 60)
    private String systemCode;

    // ========================================
    // MÉTRICAS PRINCIPAIS
    // ========================================

    @Column(name = "total_articles", nullable = false)
    private Integer totalArticles = 0;

    @Column(name = "ia_ready_count", nullable = false)
    private Integer iaReadyCount = 0;

    @Column(name = "avg_quality_score")
    private Double avgQualityScore = 0.0;

    // ========================================
    // PROBLEMAS DETECTADOS
    // ========================================

    @Column(name = "empty_count", nullable = false)
    private Integer emptyCount = 0;

    @Column(name = "short_count", nullable = false)
    private Integer shortCount = 0;

    @Column(name = "duplicate_count", nullable = false)
    private Integer duplicateCount = 0;

    @Column(name = "no_structure_count", nullable = false)
    private Integer noStructureCount = 0;

    // ========================================
    // ISSUES E ATRIBUIÇÕES
    // ========================================

    @Column(name = "open_issues_count", nullable = false)
    private Integer openIssuesCount = 0;

    @Column(name = "pending_assignments", nullable = false)
    private Integer pendingAssignments = 0;

    @Column(name = "completed_assignments", nullable = false)
    private Integer completedAssignments = 0;

    // ========================================
    // SYNC STATUS
    // ========================================

    @Column(name = "sync_ok_count")
    private Integer syncOkCount = 0;

    @Column(name = "sync_error_count")
    private Integer syncErrorCount = 0;

    @Column(name = "unclassified_count")
    private Integer unclassifiedCount = 0;

    // ========================================
    // METADATA
    // ========================================

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    // ========================================
    // MÉTODOS DE NEGÓCIO
    // ========================================

    /**
     * Calcula percentual de artigos IA-ready
     */
    public Double getIaReadyPercentage() {
        if (totalArticles == null || totalArticles == 0) {
            return 0.0;
        }
        return (iaReadyCount.doubleValue() / totalArticles.doubleValue()) * 100;
    }

    /**
     * Calcula percentual de artigos com problemas
     */
    public Double getProblemsPercentage() {
        if (totalArticles == null || totalArticles == 0) {
            return 0.0;
        }
        int problems = (emptyCount != null ? emptyCount : 0) +
                (shortCount != null ? shortCount : 0) +
                (duplicateCount != null ? duplicateCount : 0) +
                (noStructureCount != null ? noStructureCount : 0);
        return (problems * 100.0) / totalArticles.doubleValue();
    }

    /**
     * Verifica se é snapshot global (todos os sistemas)
     */
    public boolean isGlobal() {
        return systemCode == null || systemCode.isBlank();
    }

    /**
     * Calcula saúde geral (0-100)
     */
    public Double getHealthScore() {
        if (totalArticles == null || totalArticles == 0) {
            return 0.0;
        }

        // Fórmula: peso maior para IA-ready, desconto por problemas
        double iaReadyWeight = getIaReadyPercentage() * 0.7;
        double problemsPenalty = getProblemsPercentage() * 0.3;

        return Math.max(0.0, iaReadyWeight - problemsPenalty);
    }

    // ========================================
    // GETTERS E SETTERS
    // ========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public Integer getTotalArticles() {
        return totalArticles;
    }

    public void setTotalArticles(Integer totalArticles) {
        this.totalArticles = totalArticles;
    }

    public Integer getIaReadyCount() {
        return iaReadyCount;
    }

    public void setIaReadyCount(Integer iaReadyCount) {
        this.iaReadyCount = iaReadyCount;
    }

    public Double getAvgQualityScore() {
        return avgQualityScore;
    }

    public void setAvgQualityScore(Double avgQualityScore) {
        this.avgQualityScore = avgQualityScore;
    }

    public Integer getEmptyCount() {
        return emptyCount;
    }

    public void setEmptyCount(Integer emptyCount) {
        this.emptyCount = emptyCount;
    }

    public Integer getShortCount() {
        return shortCount;
    }

    public void setShortCount(Integer shortCount) {
        this.shortCount = shortCount;
    }

    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public Integer getNoStructureCount() {
        return noStructureCount;
    }

    public void setNoStructureCount(Integer noStructureCount) {
        this.noStructureCount = noStructureCount;
    }

    public Integer getOpenIssuesCount() {
        return openIssuesCount;
    }

    public void setOpenIssuesCount(Integer openIssuesCount) {
        this.openIssuesCount = openIssuesCount;
    }

    public Integer getPendingAssignments() {
        return pendingAssignments;
    }

    public void setPendingAssignments(Integer pendingAssignments) {
        this.pendingAssignments = pendingAssignments;
    }

    public Integer getCompletedAssignments() {
        return completedAssignments;
    }

    public void setCompletedAssignments(Integer completedAssignments) {
        this.completedAssignments = completedAssignments;
    }

    public Integer getSyncOkCount() {
        return syncOkCount;
    }

    public void setSyncOkCount(Integer syncOkCount) {
        this.syncOkCount = syncOkCount;
    }

    public Integer getSyncErrorCount() {
        return syncErrorCount;
    }

    public void setSyncErrorCount(Integer syncErrorCount) {
        this.syncErrorCount = syncErrorCount;
    }

    public Integer getUnclassifiedCount() {
        return unclassifiedCount;
    }

    public void setUnclassifiedCount(Integer unclassifiedCount) {
        this.unclassifiedCount = unclassifiedCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}