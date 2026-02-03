package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MovideskTicketActionRequest {

    private final String description;

    @JsonProperty("createdBy")
    private final String createdBy;

    private final int type;

    public MovideskTicketActionRequest(String description, String createdBy, int type) {
        this.description = description;
        this.createdBy = createdBy;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public int getType() {
        return type;
    }
}
