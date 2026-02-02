package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.*;
import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.exception.IntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🎫 Service de Integração com Tickets do Movidesk
 */
@Service
public class MovideskTicketService {

    private static final Logger log = LoggerFactory.getLogger(MovideskTicketService.class);

    private static final String TICKET_BASE_URL = "https://consisanet.movidesk.com/Ticket/Edit/";
    private static final String KB_BASE_URL = "https://consisanet.movidesk.com/kb/pt-br/article/";

    // ========================================
    // CONFIGURAÇÕES
    // ========================================

    @Value("${movidesk.ticket.service:Base de Conhecimento}")
    private String ticketService;

    @Value("${movidesk.ticket.category:Governança}")
    private String ticketCategory;

    @Value("${movidesk.ticket.client-id}")
    private String defaultClientId;

    @Value("${movidesk.ticket.default-team:ERP - EMPRESARIAL}")
    private String defaultTeam;

    private final MovideskClient movideskClient;

    public MovideskTicketService(MovideskClient movideskClient) {
        this.movideskClient = movideskClient;
    }

    /**
     * Cria ticket no Movidesk para uma atribuição
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MovideskTicketResponse createTicketForAssignment(
            KbArticleAssignment assignment,
            String articleTitle
    ) {
        log.info("🎫 Criando ticket para atribuição: assignmentId={} articleId={}",
                assignment.getId(), assignment.getArticleId());

        if (assignment.getTicketId() != null && !assignment.getTicketId().isBlank()) {
            log.info("🎫 Ticket já existente para assignmentId={}, ticketId={}",
                    assignment.getId(), assignment.getTicketId());
            MovideskTicketResponse existing = new MovideskTicketResponse();
            existing.setId(assignment.getTicketId());
            existing.setStatus("EXISTING");
            return existing;
        }

        // Validações
        if (defaultClientId == null || defaultClientId.isBlank()) {
            throw new IntegrationException(
                    "movidesk.ticket.client-id não está configurado. " +
                            "Configure no application.properties"
            );
        }

        // Monta dados do ticket
        String subject = buildTicketSubject(assignment, articleTitle);
        String description = buildTicketDescription(assignment, articleTitle);
        String urgency = mapPriorityToUrgency(assignment.getPriority());
        String ownerTeam = resolveOwnerTeam(assignment.getAgent());
        String ownerId = assignment.getAgent().getId();

        // ========================================
        // CORREÇÃO: Status e Category em PORTUGUÊS
        // ========================================

        MovideskTicketRequest request = new MovideskTicketRequest.Builder()
                // Básicos
                .subject(subject)
                .urgency(urgency)
                .justification(description)

                // ⚠️ OBRIGATÓRIOS
                .serviceFirstLevel(ticketService)
                .addClient(defaultClientId)
                .ownerTeam(ownerTeam)
                .owner(ownerId)
                .createdBy(ownerId)
                .addAction(description, ownerId)  // ✅ Com createdBy

                // ⚠️ CORREÇÃO: NÃO enviar status e category
                // Deixe o Movidesk usar os valores padrão
                // .category(ticketCategory)  <- COMENTADO

                // Tags
                .addTag("kb-governance")
                .addTag("atualização-manual")
                .addTag(assignment.getReason().name().toLowerCase())

                .build();

        // Cria ticket no Movidesk
        try {
            MovideskTicketResponse response = movideskClient.createTicket(request);

            log.info("✅ Ticket criado com sucesso: id={} protocol={}",
                    response.getId(), response.getProtocol());

            return response;

        } catch (Exception e) {
            log.error("❌ Erro ao criar ticket no Movidesk: {}", e.getMessage(), e);
            throw new IntegrationException("Falha ao criar ticket no Movidesk.", e);
        }
    }

    /**
     * Gera URL do ticket no Movidesk
     */
    public String buildTicketUrl(String ticketId) {
        return TICKET_BASE_URL + ticketId;
    }

    /**
     * Gera URL do artigo da KB
     */
    public String buildArticleUrl(Long articleId) {
        return KB_BASE_URL + articleId;
    }

    // =========================================================
    // HELPERS PRIVADOS
    // =========================================================

    /**
     * Resolve o time do agente
     */
    private String resolveOwnerTeam(KbAgent agent) {
        if (agent.getTeams() != null && !agent.getTeams().isEmpty()) {
            return agent.getTeams().iterator().next();
        }

        log.warn("⚠️ Agente {} não tem time configurado. Usando time padrão: {}",
                agent.getUserName(), defaultTeam);

        return defaultTeam;
    }

    /**
     * Monta assunto do ticket
     */
    private String buildTicketSubject(KbArticleAssignment assignment, String articleTitle) {
        String reasonLabel = getReasonLabel(assignment.getReason());
        String truncatedTitle = truncate(articleTitle, 80);

        return String.format("[KB] %s - %s (#%d)",
                reasonLabel,
                truncatedTitle,
                assignment.getArticleId());
    }

    /**
     * Monta descrição detalhada do ticket
     */
    private String buildTicketDescription(KbArticleAssignment assignment, String articleTitle) {
        StringBuilder sb = new StringBuilder();

        sb.append("🔧 ATUALIZAÇÃO DE MANUAL DA BASE DE CONHECIMENTO\n\n");

        // Informações do artigo
        sb.append("📄 ARTIGO:\n");
        sb.append("- ID: ").append(assignment.getArticleId()).append("\n");
        sb.append("- Título: ").append(articleTitle).append("\n");
        sb.append("- URL: ").append(buildArticleUrl(assignment.getArticleId())).append("\n\n");

        // Motivo da atribuição
        sb.append("⚠️ MOTIVO:\n");
        sb.append("- ").append(getReasonDescription(assignment.getReason())).append("\n\n");

        // Prioridade
        sb.append("🎯 PRIORIDADE: ").append(assignment.getPriority().getDescription()).append("\n\n");

        // Prazo
        if (assignment.getDueDate() != null) {
            sb.append("📅 PRAZO: ").append(assignment.getDueDate().toLocalDate()).append("\n\n");
        }

        // Descrição adicional
        if (assignment.getDescription() != null && !assignment.getDescription().isBlank()) {
            sb.append("📝 OBSERVAÇÕES:\n");
            sb.append(assignment.getDescription()).append("\n\n");
        }

        // Instruções
        sb.append("✅ AÇÃO REQUERIDA:\n");
        sb.append("1. Acessar o artigo da KB usando o link acima\n");
        sb.append("2. Revisar e atualizar o conteúdo conforme necessário\n");
        sb.append("3. Garantir qualidade e completude das informações\n");
        sb.append("4. Marcar a tarefa como concluída no sistema\n");

        return sb.toString();
    }

    /**
     * Mapeia prioridade do assignment para urgência do Movidesk
     */
    private String mapPriorityToUrgency(AssignmentPriority priority) {
        return switch (priority) {
            case URGENT -> "Urgente";
            case HIGH -> "Alta";
            case MEDIUM -> "Normal";
            case LOW -> "Baixa";
        };
    }

    /**
     * Retorna label curto do motivo
     */
    private String getReasonLabel(AssignmentReason reason) {
        return switch (reason) {
            case QUALITY_LOW -> "Baixa Qualidade";
            case CONTENT_EMPTY -> "Conteúdo Vazio";
            case CONTENT_OUTDATED -> "Desatualizado";
            case DUPLICATE -> "Duplicado";
            case NO_STRUCTURE -> "Sem Estrutura";
            case MANUAL_REQUEST -> "Solicitação Manual";
        };
    }

    /**
     * Retorna descrição completa do motivo
     */
    private String getReasonDescription(AssignmentReason reason) {
        return switch (reason) {
            case QUALITY_LOW -> "Score de qualidade abaixo do mínimo aceitável (<50)";
            case CONTENT_EMPTY -> "Artigo sem conteúdo ou com conteúdo muito reduzido";
            case CONTENT_OUTDATED -> "Conteúdo desatualizado, precisa revisão";
            case DUPLICATE -> "Artigo duplicado detectado";
            case NO_STRUCTURE -> "Artigo sem estrutura mínima (sem títulos, sem organização)";
            case MANUAL_REQUEST -> "Solicitação manual de atualização";
        };
    }

    /**
     * Trunca string para evitar assuntos muito longos
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
