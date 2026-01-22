package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO para cliente/solicitante do ticket
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskClientDto {
    
    /**
     * ID do cliente no Movidesk
     * Exemplo: "631321976" (CONSISA SISTEMAS)
     */
    private String id;
    
    /**
     * Nome do cliente (retornado pela API)
     */
    private String businessName;
    
    /**
     * Email do cliente (retornado pela API)
     */
    private String email;
    
    /**
     * Tipo de pessoa: 1 = Física, 2 = Jurídica
     */
    private Integer personType;

    // Construtores
    public MovideskClientDto() {
    }

    public MovideskClientDto(String id) {
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

    public Integer getPersonType() {
        return personType;
    }

    public void setPersonType(Integer personType) {
        this.personType = personType;
    }

    @Override
    public String toString() {
        return "MovideskClientDto{" +
                "id='" + id + '\'' +
                ", businessName='" + businessName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
