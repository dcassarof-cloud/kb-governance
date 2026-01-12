package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Entidade que representa um artigo da Knowledge Base
 * persistido no banco de dados local.
 *
 * Origem:
 * - Sincronizado a partir da API do Movidesk
 *
 * Objetivo:
 * - Centralizar artigos de conhecimento
 * - Permitir classificação por sistema/módulo
 * - Manter histórico mínimo de sincronização
 */
@Entity
@Table(name = "kb_article")
public class KbArticle {

    /**
     * ID do artigo no sistema de origem (Movidesk).
     *
     * ⚠️ Importante:
     * - Não é gerado automaticamente
     * - O próprio ID do Movidesk é usado como chave primária
     */
    @Id
    private Long id;

    /**
     * Título do artigo
     */
    @Column(nullable = false)
    private String title;

    /**
     * Slug do artigo (usado para URLs amigáveis)
     */
    private String slug;

    /**
     * Status do artigo no sistema de origem
     * Ex: ativo / inativo
     */
    @Column(name = "article_status", nullable = false)
    private Integer articleStatus;

    /**
     * Resumo do artigo
     */
    @Column(columnDefinition = "text")
    private String summary;

    /**
     * Conteúdo do artigo em HTML
     * Usado para renderização em tela
     */
    @Column(name = "content_html", columnDefinition = "text")
    private String contentHtml;

    /**
     * Conteúdo do artigo em texto puro
     * Útil para buscas, indexação ou IA futuramente
     */
    @Column(name = "content_text", columnDefinition = "text")
    private String contentText;

    /**
     * Identificador da revisão do artigo no sistema de origem
     * Ajuda a detectar mudanças entre sincronizações
     */
    @Column(name = "revision_id")
    private Long revisionId;

    /**
     * Tempo estimado de leitura (ex: "5 min")
     */
    @Column(name = "reading_time")
    private String readingTime;

    /**
     * Data de criação do artigo no sistema de origem
     */
    @Column(name = "created_date")
    private OffsetDateTime createdDate;

    /**
     * Data da última atualização no sistema de origem
     */
    @Column(name = "updated_date")
    private OffsetDateTime updatedDate;

    /**
     * Data/hora em que o artigo foi buscado/sincronizado
     * pelo nosso sistema.
     *
     * ⚠️ Importante para:
     * - Auditoria
     * - Reprocessamento
     * - Diagnóstico de falhas
     */
    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    /**
     * URL do artigo no sistema de origem
     * (ex: link direto para o Movidesk)
     */
    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    /**
     * Sistema de origem do artigo.
     * Ex: "movidesk"
     *
     * Mantido como string para permitir múltiplas origens futuramente.
     */
    @Column(name = "source_system", nullable = false)
    private String sourceSystem = "movidesk";

    /**
     * Sistema/módulo interno ao qual o artigo pertence.
     *
     * Ex:
     * - Quinto Eixo
     * - SGRH
     * - NotaOn
     *
     * Pode ser null quando o artigo ainda não foi classificado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    private KbSystem system;

    /**
     * Status do sync local.
     * Ex: OK, NOT_FOUND, ERROR
     */
    @Column(name = "sync_status", length = 30)
    private String syncStatus;

    /**
     * Mensagem de erro do sync (resumida, até 400 chars)
     * OU warnings (ex: "WARN:EMPTY_CONTENT | WARN:MENU_NULL")
     */
    @Column(name = "sync_error_message", length = 400)
    private String syncErrorMessage;

    /**
     * Nome do menu de origem (Movidesk)
     */
    @Column(name = "source_menu_name", length = 200)
    private String sourceMenuName;

    /**
     * ID do menu de origem (Movidesk)
     */
    @Column(name = "source_menu_id")
    private Long sourceMenuId;

    /**
     * Hash do conteúdo normalizado (útil para duplicados futuramente)
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /**
     * Status de governança (fluxo interno)
     * ⚠️ nullable=false -> sempre precisa estar preenchido
     */
    @Column(name = "governance_status", nullable = false)
    private String governanceStatus;

    @Column(name = "approved_revision_id")
    private Long approvedRevisionId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    /**
     * Atualiza automaticamente o campo fetchedAt
     * sempre que o registro for inserido ou atualizado.
     *
     * Garante que o horário fique em UTC.
     */
    @PrePersist
    @PreUpdate
    public void touchFetchedAt() {
        this.fetchedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "sync_state", length = 20)
    private String syncState;

    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(OffsetDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getSyncState() { return syncState; }
    public void setSyncState(String syncState) { this.syncState = syncState; }

    /* ======================
       Getters e Setters
       ====================== */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public Integer getArticleStatus() { return articleStatus; }
    public void setArticleStatus(Integer articleStatus) { this.articleStatus = articleStatus; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }

    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }

    public String getReadingTime() { return readingTime; }
    public void setReadingTime(String readingTime) { this.readingTime = readingTime; }

    public OffsetDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(OffsetDateTime createdDate) { this.createdDate = createdDate; }

    public OffsetDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(OffsetDateTime updatedDate) { this.updatedDate = updatedDate; }

    public OffsetDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(OffsetDateTime fetchedAt) { this.fetchedAt = fetchedAt; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public KbSystem getSystem() { return system; }
    public void setSystem(KbSystem system) { this.system = system; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getSyncErrorMessage() { return syncErrorMessage; }
    public void setSyncErrorMessage(String syncErrorMessage) { this.syncErrorMessage = syncErrorMessage; }

    public String getSourceMenuName() { return sourceMenuName; }
    public void setSourceMenuName(String sourceMenuName) { this.sourceMenuName = sourceMenuName; }

    public Long getSourceMenuId() { return sourceMenuId; }
    public void setSourceMenuId(Long sourceMenuId) { this.sourceMenuId = sourceMenuId; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getGovernanceStatus() { return governanceStatus; }
    public void setGovernanceStatus(String governanceStatus) { this.governanceStatus = governanceStatus; }

    public Long getApprovedRevisionId() { return approvedRevisionId; }
    public void setApprovedRevisionId(Long approvedRevisionId) { this.approvedRevisionId = approvedRevisionId; }

    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
}
