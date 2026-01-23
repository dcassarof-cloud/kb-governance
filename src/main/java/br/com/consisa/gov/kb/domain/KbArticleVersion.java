package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 📚 VERSÃO DE ARTIGO
 *
 * OBJETIVO:
 * ---------
 * Manter histórico completo de alterações em artigos da KB:
 * - Rastreabilidade: quem mudou, quando, por quê
 * - Auditoria: compliance e segurança
 * - Rollback: possibilidade de reverter mudanças
 * - Comparação: diff entre versões
 *
 * QUANDO É CRIADA:
 * ----------------
 * - Criação do artigo (versão 1)
 * - Toda atualização de conteúdo
 * - Aprovação de governança
 * - Sincronização com Movidesk
 * - Rollback (cria nova versão com conteúdo antigo)
 *
 * DADOS ARMAZENADOS:
 * ------------------
 * - Snapshot completo do artigo no momento
 * - Metadata da mudança (quem, quando, por quê)
 * - Hash do conteúdo (para detectar mudanças reais)
 */
@Entity
@Table(name = "kb_article_version",
       uniqueConstraints = @UniqueConstraint(columnNames = {"article_id", "version_number"}))
public class KbArticleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referência ao artigo original
     */
    @Column(name = "article_id", nullable = false)
    private Long articleId;

    /**
     * Número sequencial da versão (1, 2, 3...)
     */
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    // ========================================
    // SNAPSHOT DO CONTEÚDO
    // ========================================

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "summary", length = 2000)
    private String summary;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    /**
     * Hash SHA-256 do conteúdo (para detectar mudanças reais)
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    // ========================================
    // METADATA DA MUDANÇA
    // ========================================

    /**
     * Quem fez a mudança (username, email, ou "SYSTEM")
     */
    @Column(name = "changed_by", length = 100)
    private String changedBy;

    /**
     * Motivo da mudança
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * Tipo de mudança:
     * - CREATED: primeira versão
     * - UPDATED: atualização de conteúdo
     * - APPROVED: aprovação de governança
     * - REVERTED: rollback para versão anterior
     * - SYNC: sincronização com Movidesk
     */
    @Column(name = "change_type", length = 30)
    private String changeType;

    // ========================================
    // GOVERNANÇA
    // ========================================

    @Column(name = "governance_status", length = 30)
    private String governanceStatus;

    @Column(name = "quality_score")
    private Integer qualityScore;

    // ========================================
    // TIMESTAMPS
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
     * Verifica se é a primeira versão (criação)
     */
    public boolean isInitialVersion() {
        return versionNumber != null && versionNumber == 1;
    }

    /**
     * Verifica se houve mudança de título
     */
    public boolean hasTitleChange(KbArticleVersion other) {
        if (this.title == null && other.title == null) return false;
        if (this.title == null || other.title == null) return true;
        return !this.title.equals(other.title);
    }

    /**
     * Verifica se houve mudança de conteúdo (via hash)
     */
    public boolean hasContentChange(KbArticleVersion other) {
        if (this.contentHash == null && other.contentHash == null) return false;
        if (this.contentHash == null || other.contentHash == null) return true;
        return !this.contentHash.equals(other.contentHash);
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

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getGovernanceStatus() {
        return governanceStatus;
    }

    public void setGovernanceStatus(String governanceStatus) {
        this.governanceStatus = governanceStatus;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
