package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) que representa um artigo retornado
 * pela API de Knowledge Base do Movidesk.
 *
 * ⚠️ Somente mapeamento JSON → Java.
 * Nenhuma regra de negócio deve existir aqui.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskArticleDto {

    /**
     * Identificador único do artigo no Movidesk
     */
    private Long id;

    /**
     * Status do artigo no Movidesk
     * 1 = Ativo
     * 2 = Inativo
     */
    @JsonProperty("articleStatus")
    private Integer articleStatus;

    /**
     * Título do artigo
     */
    private String title;

    /**
     * Slug do artigo
     */
    private String slug;

    /**
     * Resumo do artigo
     */
    private String summary;

    /**
     * Conteúdo HTML
     */
    @JsonProperty("contentHtml")
    private String contentHtml;

    /**
     * Conteúdo em texto puro
     */
    @JsonProperty("contentText")
    private String contentText;

    /**
     * Identificador da revisão
     */
    @JsonProperty("revisionId")
    private Long revisionId;

    /**
     * Tempo estimado de leitura
     */
    @JsonProperty("readingTime")
    private String readingTime;

    /**
     * Data de criação (string da API)
     */
    @JsonProperty("createdDate")
    private String createdDate;

    /**
     * Data de última atualização (string da API)
     */
    @JsonProperty("updatedDate")
    private String updatedDate;

    /**
     * ✅ MENU DO MOVIDESK (PONTO CRÍTICO)
     */
    @JsonProperty("menu")
    private MovideskMenuDto menu;

    /* ======================
       Getters e Setters
       ====================== */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getArticleStatus() {
        return articleStatus;
    }

    public void setArticleStatus(Integer articleStatus) {
        this.articleStatus = articleStatus;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public Long getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(Long revisionId) {
        this.revisionId = revisionId;
    }

    public String getReadingTime() {
        return readingTime;
    }

    public void setReadingTime(String readingTime) {
        this.readingTime = readingTime;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public MovideskMenuDto getMenu() {
        return menu;
    }

    public void setMenu(MovideskMenuDto menu) {
        this.menu = menu;
    }
}
