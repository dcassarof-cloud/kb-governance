package br.com.consisa.gov.kb.client.movidesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO para criação de ticket no Movidesk
 *
 * ⚠️ IMPORTANTE: Status e Category são OPCIONAIS
 * Se não informados, o Movidesk usa valores padrão do processo
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)  // Não envia campos null
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
     * ⚠️ OBRIGATÓRIO: Serviço de primeiro nível
     */
    @JsonProperty("serviceFirstLevel")
    private String serviceFirstLevel;

    /**
     * Categoria (OPCIONAL - deixe null para usar padrão do processo)
     */
    private String category;

    /**
     * Urgência: Baixa, Normal, Alta, Urgente
     */
    private String urgency = "Normal";

    /**
     * Status (OPCIONAL - deixe null para usar padrão do processo)
     * Se informar, deve ser: "Novo", "EmAndamento", "Resolvido", "Fechado"
     */
    private String status;

    /**
     * Relacionamento com base de conhecimento
     */
    @JsonProperty("baseStatus")
    private String baseStatus;

    /**
     * Justificativa (descrição detalhada)
     */
    @JsonProperty("justification")
    private String justification;

    /**
     * Origem: Email, Chat, Telefone, Manual, etc
     */
    private String origin = "Manual";

    /**
     * ⚠️ OBRIGATÓRIO: Time responsável
     */
    @JsonProperty("ownerTeam")
    private String ownerTeam;

    /**
     * Tags do ticket
     */
    private List<String> tags = new ArrayList<>();

    /**
     * ⚠️ OBRIGATÓRIO: Clientes/solicitantes
     */
    private List<MovideskClientDto> clients = new ArrayList<>();

    /**
     * ⚠️ OBRIGATÓRIO: Ações (mensagens) do ticket
     */
    private List<MovideskActionDto> actions = new ArrayList<>();

    /**
     * Responsável pelo ticket (owner)
     */
    private MovideskOwnerDto owner;

    /**
     * ⚠️ OBRIGATÓRIO (em alguns fluxos do Movidesk): usuário/agente que está criando o ticket.
     *
     * Observação: isto é diferente do createdBy da Action (mensagem interna).
     */
    @JsonProperty("createdBy")
    private MovideskOwnerDto createdBy;

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

    public String getServiceFirstLevel() {
        return serviceFirstLevel;
    }

    public void setServiceFirstLevel(String serviceFirstLevel) {
        this.serviceFirstLevel = serviceFirstLevel;
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

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
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
     * Builder para facilitar criação de tickets
     */
    public static class Builder {
        private final MovideskTicketRequest ticket = new MovideskTicketRequest();

        public Builder subject(String subject) {
            ticket.subject = subject;
            return this;
        }

        public Builder serviceFirstLevel(String service) {
            ticket.serviceFirstLevel = service;
            return this;
        }

        /**
         * OPCIONAL: Define categoria
         * ⚠️ Se não chamar, o Movidesk usa o padrão do processo
         */
        public Builder category(String category) {
            ticket.category = category;
            return this;
        }

        public Builder urgency(String urgency) {
            ticket.urgency = urgency;
            return this;
        }

        /**
         * OPCIONAL: Define status
         * ⚠️ Se não chamar, o Movidesk usa "Novo"
         */
        public Builder status(String status) {
            ticket.status = status;
            return this;
        }

        public Builder justification(String justification) {
            ticket.justification = justification;
            return this;
        }

        public Builder ownerTeam(String team) {
            ticket.ownerTeam = team;
            return this;
        }

        public Builder owner(String id) {
            MovideskOwnerDto owner = new MovideskOwnerDto();
            owner.setId(id);
            ticket.owner = owner;
            return this;
        }

        /**
         * ⚠️ OBRIGATÓRIO (em alguns fluxos do Movidesk): define o CreatedBy do ticket.
         * Isso representa "quem abriu o ticket" (não confundir com Action.createdBy).
         */
        public Builder createdBy(String id) {
            MovideskOwnerDto cb = new MovideskOwnerDto();
            cb.setId(id);
            ticket.createdBy = cb;
            return this;
        }

        public Builder addClient(String clientId) {
            MovideskClientDto client = new MovideskClientDto();
            client.setId(clientId);
            ticket.clients.add(client);
            return this;
        }

        public Builder addTag(String tag) {
            ticket.tags.add(tag);
            return this;
        }

        /**
         * ⚠️ OBRIGATÓRIO: Adiciona ação ao ticket
         *
         * @param description texto da ação
         * @param createdById ID do agente que criou
         */
        public Builder addAction(String description, String createdById) {
            MovideskActionDto action = new MovideskActionDto();
            action.setType(1); // Tipo 1 = Ação interna
            action.setDescription(description);

            MovideskOwnerDto createdBy = new MovideskOwnerDto();
            createdBy.setId(createdById);
            action.setCreatedBy(createdBy);

            ticket.actions.add(action);
            return this;
        }

        public MovideskTicketRequest build() {
            // Validações básicas
            if (ticket.subject == null || ticket.subject.isBlank()) {
                throw new IllegalStateException("Subject é obrigatório");
            }

            if (ticket.serviceFirstLevel == null || ticket.serviceFirstLevel.isBlank()) {
                throw new IllegalStateException("ServiceFirstLevel é obrigatório");
            }

            // Alguns fluxos do Movidesk exigem CreatedBy no nível do ticket (além da Action).
            if (ticket.createdBy == null || ticket.createdBy.getId() == null || ticket.createdBy.getId().isBlank()) {
                throw new IllegalStateException("CreatedBy do ticket é obrigatório");
            }

            if (ticket.actions.isEmpty()) {
                throw new IllegalStateException("Pelo menos 1 action é obrigatória");
            }

            if (ticket.clients.isEmpty()) {
                throw new IllegalStateException("Pelo menos 1 client é obrigatório");
            }

            if (ticket.ownerTeam == null && ticket.owner == null) {
                throw new IllegalStateException("OwnerTeam ou Owner é obrigatório");
            }

            return ticket;
        }
    }
}