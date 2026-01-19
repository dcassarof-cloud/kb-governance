package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO que representa o menu do Movidesk
 * Ex:
 * {
 *   "id": 13281,
 *   "name": "Consisanet - Faturamento"
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskMenuDto {

    private Long id;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
