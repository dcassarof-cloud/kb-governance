package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.MovideskTicketResponse;
import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.exception.IntegrationException;
import br.com.consisa.gov.kb.repository.KbArticleAssignmentRepository;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 📋 Service de Gerenciamento de Atribuições
 *
 * RESPONSABILIDADES:
 * ------------------
 * - Criar atribuições manuais ou automáticas
 * - Criar tickets no Movidesk automaticamente
 * - Atualizar status de atribuições
 * - Buscar atribuições por filtros
 * - Gerenciar ciclo de vida (pending → in_progress → completed)
 *
 * INTEGRAÇÃO:
 * -----------
 * - KbAgentService: seleção e atualização de agentes
 * - MovideskTicketService: criação de tickets
 * - KbArticleRepository: busca informações do artigo
 */
@Service
public class KbAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(KbAssignmentService.class);

    private final KbArticleAssignmentRepository repository;
    private final KbAgentService agentService;
    private final MovideskTicketService ticketService;
    private final KbArticleRepository articleRepository;

    public KbAssignmentService(
            KbArticleAssignmentRepository repository,
            KbAgentService agentService,
            MovideskTicketService ticketService,
            KbArticleRepository articleRepository
    ) {
        this.repository = repository;
        this.agentService = agentService;
        this.ticketService = ticketService;
        this.articleRepository = articleRepository;
    }

    /**
     * ➕ Cria nova atribuição manual COM TICKET NO MOVIDESK
     *
     * @param articleId ID do artigo
     * @param agentId ID do agente
     * @param reason motivo da atribuição
     * @param priority prioridade
     * @param dueDate prazo (opcional)
     * @param description descrição adicional (opcional)
     * @param createTicket se deve criar ticket no Movidesk
     * @return atribuição criada
     */
    @Transactional(noRollbackFor = IntegrationException.class)
    public KbArticleAssignment createManualAssignment(
            Long articleId,
            String agentId,
            AssignmentReason reason,
            AssignmentPriority priority,
            OffsetDateTime dueDate,
            String description,
            boolean createTicket
    ) {
        log.info("➕ Criando atribuição manual: article={} agent={} createTicket={}",
                articleId, agentId, createTicket);

        // Verifica se já existe atribuição ativa
        if (repository.existsActiveByArticleId(articleId)) {
            throw new IllegalStateException(
                    "Artigo " + articleId + " já possui atribuição ativa"
            );
        }

        // Busca agente
        KbAgent agent = agentService.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Agente não encontrado: " + agentId
                ));

        if (!agent.getIsActive()) {
            throw new IllegalStateException(
                    "Agente " + agent.getUserName() + " está inativo"
            );
        }

        // Busca título do artigo
        String articleTitle = articleRepository.findById(articleId)
                .map(KbArticle::getTitle)
                .orElse("Artigo #" + articleId);

        // Cria atribuição
        KbArticleAssignment assignment = new KbArticleAssignment();
        assignment.setArticleId(articleId);
        assignment.setAgent(agent);
        assignment.setReason(reason);
        assignment.setPriority(priority);
        assignment.setDueDate(dueDate);
        assignment.setDescription(description);
        assignment.setStatus(AssignmentStatus.PENDING);
        assignment.setAssignedBy("MANUAL");
        assignment.setTicketStatus(createTicket ? TicketStatus.PENDING : TicketStatus.NONE);

        // Salva primeiro (para ter ID)
        KbArticleAssignment saved = repository.save(assignment);

        // Cria ticket no Movidesk se solicitado
        if (createTicket) {
            try {
                MovideskTicketResponse ticketResponse = ticketService.createTicketForAssignment(
                        saved,
                        articleTitle
                );

                // Atualiza assignment com dados do ticket
                saved.setTicketId(ticketResponse.getId());
                saved.setTicketUrl(ticketService.buildTicketUrl(ticketResponse.getId()));
                saved.setTicketStatus(TicketStatus.CREATED);
                saved.setTicketCreatedAt(OffsetDateTime.now());
                saved.setTicketLastError(null);

                repository.save(saved);

                log.info("🎫 Ticket criado e vinculado: ticketId={}", ticketResponse.getId());

            } catch (IntegrationException e) {
                log.warn("❌ Falha ao criar ticket (atribuição mantida): {}", e.getMessage());
                markTicketFailure(saved, e.getMessage());
                repository.save(saved);
                throw e;
            }
        }

        // Atualiza contador do agente
        agentService.incrementAssigned(agentId);

        log.info("✅ Atribuição criada: id={} ticket={}", saved.getId(), saved.getTicketId());

        return saved;
    }

    /**
     * 🤖 Cria atribuição automática baseada em especialidade COM TICKET
     *
     * @param articleId ID do artigo
     * @param systemCode código do sistema do artigo
     * @param reason motivo da atribuição
     * @param qualityScore score de qualidade (para determinar prioridade)
     * @param createTicket se deve criar ticket no Movidesk
     * @return atribuição criada
     */
    @Transactional(noRollbackFor = IntegrationException.class)
    public KbArticleAssignment createAutoAssignment(
            Long articleId,
            String systemCode,
            AssignmentReason reason,
            Integer qualityScore,
            boolean createTicket
    ) {
        log.info("🤖 Criando atribuição automática: article={} system={} createTicket={}",
                articleId, systemCode, createTicket);

        // Verifica se já existe atribuição ativa
        if (repository.existsActiveByArticleId(articleId)) {
            log.warn("⚠️ Artigo {} já possui atribuição ativa. Pulando.", articleId);
            throw new IllegalStateException(
                    "Artigo " + articleId + " já possui atribuição ativa"
            );
        }

        // Seleciona melhor agente
        KbAgent agent = agentService.findBestAgentForSystem(systemCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhum agente disponível para sistema " + systemCode
                ));

        // Determina prioridade baseada no score
        AssignmentPriority priority = AssignmentPriority.fromQualityScore(qualityScore);

        // Calcula prazo baseado na prioridade
        OffsetDateTime dueDate = calculateDueDate(priority);

        // Busca título do artigo
        String articleTitle = articleRepository.findById(articleId)
                .map(KbArticle::getTitle)
                .orElse("Artigo #" + articleId);

        // Cria atribuição
        KbArticleAssignment assignment = new KbArticleAssignment();
        assignment.setArticleId(articleId);
        assignment.setAgent(agent);
        assignment.setReason(reason);
        assignment.setPriority(priority);
        assignment.setDueDate(dueDate);
        assignment.setDescription("Atribuição automática (score: " + qualityScore + ")");
        assignment.setStatus(AssignmentStatus.PENDING);
        assignment.setAssignedBy("AUTO_ASSIGN");
        assignment.setTicketStatus(createTicket ? TicketStatus.PENDING : TicketStatus.NONE);

        // Salva primeiro
        KbArticleAssignment saved = repository.save(assignment);

        // Cria ticket no Movidesk se solicitado
        if (createTicket) {
            try {
                MovideskTicketResponse ticketResponse = ticketService.createTicketForAssignment(
                        saved,
                        articleTitle
                );

                saved.setTicketId(ticketResponse.getId());
                saved.setTicketUrl(ticketService.buildTicketUrl(ticketResponse.getId()));
                saved.setTicketStatus(TicketStatus.CREATED);
                saved.setTicketCreatedAt(OffsetDateTime.now());
                saved.setTicketLastError(null);

                repository.save(saved);

                log.info("🎫 Ticket auto criado: ticketId={}", ticketResponse.getId());

            } catch (IntegrationException e) {
                log.warn("❌ Falha ao criar ticket automático: {}", e.getMessage());
                markTicketFailure(saved, e.getMessage());
                repository.save(saved);
                throw e;
            }
        }

        // Atualiza contador do agente
        agentService.incrementAssigned(agent.getId());

        log.info("✅ Atribuição automática criada: id={} agent={} priority={}",
                saved.getId(), agent.getUserName(), priority);

        return saved;
    }

    /**
     * ▶️ Inicia atribuição (muda para IN_PROGRESS)
     */
    @Transactional
    public KbArticleAssignment startAssignment(Long assignmentId) {
        KbArticleAssignment assignment = repository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Atribuição não encontrada: " + assignmentId
                ));

        assignment.start();
        repository.save(assignment);

        log.info("▶️ Atribuição iniciada: id={}", assignmentId);

        return assignment;
    }

    /**
     * ✅ Marca atribuição como concluída
     */
    @Transactional
    public KbArticleAssignment completeAssignment(Long assignmentId, String completionNote) {
        KbArticleAssignment assignment = repository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Atribuição não encontrada: " + assignmentId
                ));

        assignment.complete(completionNote);
        repository.save(assignment);

        // Atualiza estatísticas do agente
        if (assignment.getAgent() != null) {
            agentService.registerCompletion(assignment.getAgent().getId());
        }

        log.info("✅ Atribuição concluída: id={} agent={} dias={}",
                assignmentId,
                assignment.getAgent().getUserName(),
                assignment.getCompletionDays());

        return assignment;
    }

    /**
     * ❌ Cancela atribuição
     */
    @Transactional
    public KbArticleAssignment cancelAssignment(Long assignmentId, String reason) {
        KbArticleAssignment assignment = repository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Atribuição não encontrada: " + assignmentId
                ));

        assignment.cancel(reason);
        repository.save(assignment);

        // Atualiza contador do agente
        if (assignment.getAgent() != null) {
            agentService.decrementAssigned(assignment.getAgent().getId());
        }

        log.info("❌ Atribuição cancelada: id={} motivo={}", assignmentId, reason);

        return assignment;
    }

    /**
     * 🔍 Lista atribuições por status
     */
    @Transactional(readOnly = true)
    public List<KbArticleAssignment> findByStatus(AssignmentStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * ⏰ Lista atribuições atrasadas
     */
    @Transactional(readOnly = true)
    public List<KbArticleAssignment> findOverdue() {
        return repository.findOverdue(OffsetDateTime.now());
    }

    /**
     * 📊 Busca atribuição ativa de um artigo
     */
    @Transactional(readOnly = true)
    public Optional<KbArticleAssignment> findActiveByArticle(Long articleId) {
        return repository.findActiveByArticleId(articleId);
    }

    /**
     * 🎫 Tenta criar ticket para uma atribuição existente (idempotente).
     */
    @Transactional(noRollbackFor = IntegrationException.class)
    public KbArticleAssignment createTicketForAssignment(Long assignmentId) {
        KbArticleAssignment assignment = repository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Atribuição não encontrada: " + assignmentId
                ));

        if (assignment.getTicketId() != null && !assignment.getTicketId().isBlank()) {
            log.info("🎫 Ticket já existe para assignmentId={}", assignmentId);
            if (assignment.getTicketStatus() != TicketStatus.CREATED) {
                assignment.setTicketStatus(TicketStatus.CREATED);
                assignment.setTicketLastError(null);
                repository.save(assignment);
            }
            return assignment;
        }

        String articleTitle = articleRepository.findById(assignment.getArticleId())
                .map(KbArticle::getTitle)
                .orElse("Artigo #" + assignment.getArticleId());

        assignment.setTicketStatus(TicketStatus.PENDING);
        repository.save(assignment);

        try {
            MovideskTicketResponse ticketResponse = ticketService.createTicketForAssignment(
                    assignment,
                    articleTitle
            );

            assignment.setTicketId(ticketResponse.getId());
            assignment.setTicketUrl(ticketService.buildTicketUrl(ticketResponse.getId()));
            assignment.setTicketStatus(TicketStatus.CREATED);
            assignment.setTicketCreatedAt(OffsetDateTime.now());
            assignment.setTicketLastError(null);

            repository.save(assignment);

            return assignment;
        } catch (IntegrationException e) {
            log.warn("❌ Falha ao criar ticket para assignmentId={}: {}", assignmentId, e.getMessage());
            markTicketFailure(assignment, e.getMessage());
            repository.save(assignment);
            throw e;
        }
    }

    /**
     * 📈 Retorna estatísticas agregadas
     */
    @Transactional(readOnly = true)
    public AssignmentStatistics getStatistics() {
        List<Object[]> resultList = repository.getStatistics();

        AssignmentStatistics result = new AssignmentStatistics();

        if (resultList.isEmpty()) {
            result.total = 0L;
            result.pending = 0L;
            result.inProgress = 0L;
            result.completed = 0L;
            result.cancelled = 0L;
        } else {
            Object[] stats = resultList.get(0);
            result.total = ((Number) stats[0]).longValue();
            result.pending = ((Number) stats[1]).longValue();
            result.inProgress = ((Number) stats[2]).longValue();
            result.completed = ((Number) stats[3]).longValue();
            result.cancelled = ((Number) stats[4]).longValue();
        }

        // Busca total de agentes ativos
        result.activeAgents = agentService.countActiveAgents();

        return result;
    }

    // =========================================================
    // HELPERS PRIVADOS
    // =========================================================

    /**
     * Calcula prazo baseado na prioridade
     */
    private OffsetDateTime calculateDueDate(AssignmentPriority priority) {
        OffsetDateTime now = OffsetDateTime.now();

        return switch (priority) {
            case URGENT -> now.plusDays(2);
            case HIGH -> now.plusDays(5);
            case MEDIUM -> now.plusDays(10);
            case LOW -> now.plusDays(15);
        };
    }

    private void markTicketFailure(KbArticleAssignment assignment, String error) {
        assignment.setTicketStatus(TicketStatus.FAILED);
        assignment.setTicketLastError(error);
        assignment.setTicketRetryCount(
                assignment.getTicketRetryCount() != null ? assignment.getTicketRetryCount() + 1 : 1
        );
    }

    // =========================================================
    // DTOs
    // =========================================================

    public static class AssignmentStatistics {
        public long total;
        public long pending;
        public long inProgress;
        public long completed;
        public long cancelled;
        public long activeAgents;
    }
}
