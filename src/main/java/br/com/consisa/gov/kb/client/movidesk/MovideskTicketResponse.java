package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para resposta da API do Movidesk ao criar/consultar ticket
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskTicketResponse {
    
    /**
     * ID único do ticket no Movidesk
     */
    private String id;
    
    /**
     * Protocolo do ticket (visível para o usuário)
     * Exemplo: "2026012201"
     */
    private String protocol;
    
    /**
     * Assunto do ticket
     */
    private String subject;
    
    /**
     * Tipo: 1 = Interno, 2 = Público
     */
    private Integer type;
    
    /**
     * Status: Novo, EmAndamento, Resolvido, Fechado
     */
    private String status;
    
    /**
     * Categoria
     */
    private String category;
    
    /**
     * Urgência
     */
    private String urgency;
    
    /**
     * Serviço de primeiro nível
     */
    @JsonProperty("serviceFirstLevel")
    private String serviceFirstLevel;
    
    /**
     * Data de criação
     */
    @JsonProperty("createdDate")
    private LocalDateTime createdDate;
    
    /**
     * Última atualização
     */
    @JsonProperty("lastUpdate")
    private LocalDateTime lastUpdate;
    
    /**
     * Cliente/solicitante
     */
    private List<MovideskClientDto> clients;
    
    /**
     * Responsável
     */
    private MovideskOwnerDto owner;
    
    /**
     * Time responsável
     */
    @JsonProperty("ownerTeam")
    private String ownerTeam;
    
    /**
     * Ações do ticket
     */
    private List<MovideskActionDto> actions;

    // Construtores
    public MovideskTicketResponse() {
    }

    // Getters e Setters
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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getServiceFirstLevel() {
        return serviceFirstLevel;
    }

    public void setServiceFirstLevel(String serviceFirstLevel) {
        this.serviceFirstLevel = serviceFirstLevel;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<MovideskClientDto> getClients() {
        return clients;
    }

    public void setClients(List<MovideskClientDto> clients) {
        this.clients = clients;
    }

    public MovideskOwnerDto getOwner() {
        return owner;
    }

    public void setOwner(MovideskOwnerDto owner) {
        this.owner = owner;
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    public List<MovideskActionDto> getActions() {
        return actions;
    }

    public void setActions(List<MovideskActionDto> actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        return "MovideskTicketResponse{" +
                "id='" + id + '\'' +
                ", protocol='" + protocol + '\'' +
                ", subject='" + subject + '\'' +
                ", status='" + status + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}
