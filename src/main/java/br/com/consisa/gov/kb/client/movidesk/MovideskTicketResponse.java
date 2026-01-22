package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO para resposta de ticket criado no Movidesk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskTicketResponse {

    private String id;
    private String protocol;
    private String subject;
    private String status;
    private String createdDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }
}

/**
 * DTO para cliente/solicitante do ticket
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class MovideskClientDto {
    private String id;
    private String businessName;
    private String email;

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
}

/**
 * DTO para ação/mensagem do ticket
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class MovideskActionDto {
    private Integer type; // 1 = Ação, 2 = Email, etc
    private String description;
    private String htmlDescription;

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
}

/**
 * DTO para responsável (owner) do ticket
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class MovideskOwnerDto {
    private String id;
    private String businessName;
    private String email;

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
}