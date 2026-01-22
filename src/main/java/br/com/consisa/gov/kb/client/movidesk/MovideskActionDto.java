package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para ação/mensagem do ticket no Movidesk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskActionDto {
    
    /**
     * Tipo da ação:
     * 1 = Ação/Nota INTERNA
     * 2 = Resposta PÚBLICA (email)
     */
    private Integer type;
    
    /**
     * Descrição/conteúdo da ação
     */
    private String description;
    
    /**
     * Descrição em HTML
     */
    @JsonProperty("htmlDescription")
    private String htmlDescription;
    
    /**
     * Quem criou a ação (obrigatório)
     */
    @JsonProperty("createdBy")
    private MovideskOwnerDto createdBy;

    // Construtores
    public MovideskActionDto() {
    }

    public MovideskActionDto(Integer type, String description, MovideskOwnerDto createdBy) {
        this.type = type;
        this.description = description;
        this.createdBy = createdBy;
    }

    // Getters e Setters
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlDescription() {
        return htmlDescription;
    }

    public void setHtmlDescription(String htmlDescription) {
        this.htmlDescription = htmlDescription;
    }

    public MovideskOwnerDto getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(MovideskOwnerDto createdBy) {
        this.createdBy = createdBy;
    }
}
