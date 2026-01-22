package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.service.KbAgentService;
import br.com.consisa.gov.kb.service.KbAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 👥 Controller de Agentes e Atribuições
 *
 * ENDPOINTS DISPONÍVEIS:
 * ----------------------
 * GET  /kb/agents                          - Lista todos os agentes
 * GET  /kb/agents/{id}                     - Busca agente específico
 * GET  /kb/agents/specialty/{systemCode}   - Agentes por especialidade
 * GET  /kb/agents/top/{limit}              - Top agentes mais produtivos
 *
 * GET  /kb/assignments                     - Lista atribuições (com filtro)
 * GET  /kb/assignments/{id}                - Busca atribuição específica
 * POST /kb/assignments/manual              - Cria atribuição manual
 * POST /kb/assignments/auto                - Cria atribuição automática
 * POST /kb/assignments/{id}/start          - Inicia atribuição
 * POST /kb/assignments/{id}/complete       - Conclui atribuição
 * POST /kb/assignments/{id}/cancel         - Cancela atribuição
 * GET  /kb/assignments/statistics          - Estatísticas agregadas
 * GET  /kb/assignments/overdue             - Atribuições atrasadas
 */
@RestController
@RequestMapping("/kb")
@CrossOrigin(origins = "*")
public class KbAssignmentController {

    private static final Logger log = LoggerFactory.getLogger(KbAssignmentController.class);

    private final KbAgentService agentService;
    private final KbAssignmentService assignmentService;

    public KbAssignmentController(
            KbAgentService agentService,
            KbAssignmentService assignmentService
    ) {
        this.agentService = agentService;
        this.assignmentService = assignmentService;
    }

    // ======================
    // ENDPOINTS: AGENTES
    // ======================

    /**
     * 📋 Lista todos os agentes ativos
     *
     * GET /kb/agents
     */
    @GetMapping("/agents")
    public ResponseEntity<List<KbAgent>> getAllAgents() {
        log.info("GET /kb/agents");
        return ResponseEntity.ok(agentService.findAllActive());
    }

    /**
     * 🔍 Busca agente por ID
     *
     * GET /kb/agents/{id}
     */
    @GetMapping("/agents/{id}")
    public ResponseEntity<KbAgent> getAgent(@PathVariable String id) {
        log.info("GET /kb/agents/{}", id);

        return agentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 🎯 Busca agentes por especialidade
     *
     * GET /kb/agents/specialty/{systemCode}
     *
     * Exemplo: GET /kb/agents/specialty/CONSISANET
     */
    @GetMapping("/agents/specialty/{systemCode}")
    public ResponseEntity<KbAgent> getAgentBySpecialty(@PathVariable String systemCode) {
        log.info("GET /kb/agents/specialty/{}", systemCode);

        return agentService.findBestAgentForSystem(systemCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 🏆 Busca top N agentes mais produtivos
     *
     * GET /kb/agents/top/{limit}
     *
     * Exemplo: GET /kb/agents/top/10
     */
    @GetMapping("/agents/top/{limit}")
    public ResponseEntity<List<KbAgent>> getTopAgents(@PathVariable int limit) {
        log.info("GET /kb/agents/top/{}", limit);
        return ResponseEntity.ok(agentService.findTopProductive(limit));
    }

    // ======================
    // ENDPOINTS: ATRIBUIÇÕES
    // ======================

    /**
     * 📋 Lista atribuições com filtro opcional
     *
     * GET /kb/assignments?status=PENDING
     */
    @GetMapping("/assignments")
    public ResponseEntity<List<KbArticleAssignment>> getAssignments(
            @RequestParam(required = false) AssignmentStatus status
    ) {
        log.info("GET /kb/assignments?status={}", status);

        if (status != null) {
            return ResponseEntity.ok(assignmentService.findByStatus(status));
        }

        // TODO: retornar todas se não houver filtro
        return ResponseEntity.ok(assignmentService.findByStatus(AssignmentStatus.PENDING));
    }

    /**
     * 🔍 Busca atribuição por ID
     *
     * GET /kb/assignments/{id}
     */
    @GetMapping("/assignments/{id}")
    public ResponseEntity<KbArticleAssignment> getAssignment(@PathVariable Long id) {
        log.info("GET /kb/assignments/{}", id);

        // TODO: implementar findById no service
        return ResponseEntity.notFound().build();
    }

    /**
     * ➕ Cria atribuição manual
     *
     * POST /kb/assignments/manual
     *
     * Body:
     * {
     *   "articleId": 12345,
     *   "agentId": "1363711210",
     *   "reason": "QUALITY_LOW",
     *   "priority": "HIGH",
     *   "dueDate": "2026-01-30T00:00:00Z",
     *   "description": "Artigo precisa atualização urgente",
     *   "createTicket": true
     * }
     */
    @PostMapping("/assignments/manual")
    public ResponseEntity<KbArticleAssignment> createManualAssignment(
            @RequestBody ManualAssignmentRequest request
    ) {
        log.info("POST /kb/assignments/manual: article={} agent={} createTicket={}",
                request.articleId, request.agentId, request.createTicket);

        try {
            KbArticleAssignment assignment = assignmentService.createManualAssignment(
                    request.articleId,
                    request.agentId,
                    request.reason,
                    request.priority,
                    request.dueDate,
                    request.description,
                    request.createTicket != null ? request.createTicket : true // default = true
            );

            return ResponseEntity.ok(assignment);

        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("❌ Erro ao criar atribuição: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 🤖 Cria atribuição automática
     *
     * POST /kb/assignments/auto
     *
     * Body:
     * {
     *   "articleId": 12345,
     *   "systemCode": "CONSISANET",
     *   "reason": "QUALITY_LOW",
     *   "qualityScore": 35,
     *   "createTicket": true
     * }
     */
    @PostMapping("/assignments/auto")
    public ResponseEntity<KbArticleAssignment> createAutoAssignment(
            @RequestBody AutoAssignmentRequest request
    ) {
        log.info("POST /kb/assignments/auto: article={} system={} createTicket={}",
                request.articleId, request.systemCode, request.createTicket);

        try {
            KbArticleAssignment assignment = assignmentService.createAutoAssignment(
                    request.articleId,
                    request.systemCode,
                    request.reason,
                    request.qualityScore,
                    request.createTicket != null ? request.createTicket : true // default = true
            );

            return ResponseEntity.ok(assignment);

        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("❌ Erro ao criar atribuição automática: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ▶️ Inicia atribuição (PENDING → IN_PROGRESS)
     *
     * POST /kb/assignments/{id}/start
     */
    @PostMapping("/assignments/{id}/start")
    public ResponseEntity<KbArticleAssignment> startAssignment(@PathVariable Long id) {
        log.info("POST /kb/assignments/{}/start", id);

        try {
            KbArticleAssignment assignment = assignmentService.startAssignment(id);
            return ResponseEntity.ok(assignment);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * ✅ Conclui atribuição (→ COMPLETED)
     *
     * POST /kb/assignments/{id}/complete
     *
     * Body:
     * {
     *   "completionNote": "Artigo atualizado com sucesso"
     * }
     */
    @PostMapping("/assignments/{id}/complete")
    public ResponseEntity<KbArticleAssignment> completeAssignment(
            @PathVariable Long id,
            @RequestBody(required = false) CompletionRequest request
    ) {
        log.info("POST /kb/assignments/{}/complete", id);

        try {
            String note = (request != null) ? request.completionNote : null;
            KbArticleAssignment assignment = assignmentService.completeAssignment(id, note);
            return ResponseEntity.ok(assignment);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * ❌ Cancela atribuição (→ CANCELLED)
     *
     * POST /kb/assignments/{id}/cancel
     *
     * Body:
     * {
     *   "reason": "Artigo foi removido do Movidesk"
     * }
     */
    @PostMapping("/assignments/{id}/cancel")
    public ResponseEntity<KbArticleAssignment> cancelAssignment(
            @PathVariable Long id,
            @RequestBody(required = false) CancellationRequest request
    ) {
        log.info("POST /kb/assignments/{}/cancel", id);

        try {
            String reason = (request != null) ? request.reason : "Cancelado";
            KbArticleAssignment assignment = assignmentService.cancelAssignment(id, reason);
            return ResponseEntity.ok(assignment);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 📊 Retorna estatísticas de atribuições
     *
     * GET /kb/assignments/statistics
     */
    @GetMapping("/assignments/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("GET /kb/assignments/statistics");

        var stats = assignmentService.getStatistics();

        return ResponseEntity.ok(Map.of(
                "total", stats.total,
                "pending", stats.pending,
                "inProgress", stats.inProgress,
                "completed", stats.completed,
                "cancelled", stats.cancelled,
                "activeAgents", agentService.countActive()
        ));
    }

    /**
     * ⏰ Retorna atribuições atrasadas
     *
     * GET /kb/assignments/overdue
     */
    @GetMapping("/assignments/overdue")
    public ResponseEntity<List<KbArticleAssignment>> getOverdue() {
        log.info("GET /kb/assignments/overdue");
        return ResponseEntity.ok(assignmentService.findOverdue());
    }

    // ======================
    // DTOs DE REQUEST
    // ======================

    public static class ManualAssignmentRequest {
        public Long articleId;
        public String agentId;
        public AssignmentReason reason;
        public AssignmentPriority priority;
        public OffsetDateTime dueDate;
        public String description;
        public Boolean createTicket; // Se deve criar ticket no Movidesk
    }

    public static class AutoAssignmentRequest {
        public Long articleId;
        public String systemCode;
        public AssignmentReason reason;
        public Integer qualityScore;
        public Boolean createTicket; // Se deve criar ticket no Movidesk
    }

    public static class CompletionRequest {
        public String completionNote;
    }

    public static class CancellationRequest {
        public String reason;
    }
}