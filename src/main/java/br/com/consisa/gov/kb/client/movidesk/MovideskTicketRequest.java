package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO para criação de ticket no Movidesk
 *
 * Documentação: https://api.movidesk.com/public/v1/tickets
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovideskTicketRequest {

    /**
     * Tipo do ticket: 1 = Interno, 2 = Público
     */
    private Integer type = 1;

    /**
     * Assunto do ticket
     */
    private String subject;

    /**
     * Categoria do ticket
     */
    private String category;

    /**
     * Urgência: Baixa, Normal, Alta, Urgente
     */
    private String urgency = "Normal";

    /**
     * Status: Novo, EmAndamento, Resolvido, Fechado
     */
    private String status = "Novo";

    /**
     * Relacionamento com base de conhecimento
     */
    @JsonProperty("baseStatus")
    private String baseStatus;

    /**
     * Justificativa
     */
    @JsonProperty("justification")
    private String justification;

    /**
     * Origem: Email, Chat, Telefone, Manual, etc
     */
    private String origin = "Manual";

    /**
     * Tags do ticket
     */
    private List<String> tags = new ArrayList<>();

    /**
     * Clientes/solicitantes
     */
    private List<MovideskClientDto> clients = new ArrayList<>();

    /**
     * Ações (mensagens) do ticket
     */
    private List<MovideskActionDto> actions = new ArrayList<>();

    /**
     * Responsável pelo ticket (owner)
     */
    private MovideskOwnerDto owner;

    // ======================
    // Getters e Setters
    // ======================

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBaseStatus() {
        return baseStatus;
    }

    public void setBaseStatus(String baseStatus) {
        this.baseStatus = baseStatus;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<MovideskClientDto> getClients() {
        return clients;
    }

    public void setClients(List<MovideskClientDto> clients) {
        this.clients = clients;
    }

    public List<MovideskActionDto> getActions() {
        return actions;
    }

    public void setActions(List<MovideskActionDto> actions) {
        this.actions = actions;
    }

    public MovideskOwnerDto getOwner() {
        return owner;
    }

    public void setOwner(MovideskOwnerDto owner) {
        this.owner = owner;
    }

    /**
     * Builder para facilitar criação
     */
    public static class Builder {
        private final MovideskTicketRequest ticket = new MovideskTicketRequest();

        public Builder subject(String subject) {
            ticket.subject = subject;
            return this;
        }

        public Builder category(String category) {
            ticket.category = category;
            return this;
        }

        public Builder urgency(String urgency) {
            ticket.urgency = urgency;
            return this;
        }

        public Builder justification(String justification) {
            ticket.justification = justification;
            return this;
        }

        public Builder addTag(String tag) {
            ticket.tags.add(tag);
            return this;
        }

        public Builder addAction(String description) {
            MovideskActionDto action = new MovideskActionDto();
            action.setType(1); // Tipo 1 = Ação/Descrição
            action.setDescription(description);
            ticket.actions.add(action);
            return this;
        }

        public Builder owner(String id) {
            MovideskOwnerDto owner = new MovideskOwnerDto();
            owner.setId(id);
            ticket.owner = owner;
            return this;
        }

        public MovideskTicketRequest build() {
            return ticket;
        }
    }
}