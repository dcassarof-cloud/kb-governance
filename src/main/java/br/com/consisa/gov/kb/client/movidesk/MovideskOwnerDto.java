package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO para owner/agente no Movidesk
 * Usado em: ticket.owner, action.createdBy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskOwnerDto {
    
    /**
     * ID do agente no Movidesk
     * Exemplo: "1974501910" (Ariston Deluchi)
     */
    private String id;
    
    /**
     * Nome do agente (retornado pela API)
     */
    private String businessName;
    
    /**
     * Email do agente (retornado pela API)
     */
    private String email;

    // Construtores
    public MovideskOwnerDto() {
    }

    public MovideskOwnerDto(String id) {
        this.id = id;
    }

    // Getters e Setters
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "MovideskOwnerDto{" +
                "id='" + id + '\'' +
                ", businessName='" + businessName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
