package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskArticleSearchItemDto {

    private Long id;
    private String title;
    private String summary;
    private Integer status;

    // 🔑 AQUI está o pulo do gato
    private MovideskMenuDto menu;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public MovideskMenuDto getMenu() {
        return menu;
    }

    public void setMenu(MovideskMenuDto menu) {
        this.menu = menu;
    }
}
