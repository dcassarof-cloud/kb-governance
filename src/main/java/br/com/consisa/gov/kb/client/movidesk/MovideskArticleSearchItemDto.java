package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskArticleSearchItemDto {

    private Long id;
    private String title;
    private String summary;
    private Integer status;

    /**
     * Data de atualização no search do Movidesk (string)
     * Ex: 2026-01-12T10:30:00Z
     */
    private String updatedDate;

    /**
     * Id de revisão no search (pode existir ou não, pode ser string numérica)
     */
    private String revisionId;

    private MovideskMenuDto menu;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(String updatedDate) { this.updatedDate = updatedDate; }

    public String getRevisionId() { return revisionId; }
    public void setRevisionId(String revisionId) { this.revisionId = revisionId; }

    public MovideskMenuDto getMenu() { return menu; }
    public void setMenu(MovideskMenuDto menu) { this.menu = menu; }
}
