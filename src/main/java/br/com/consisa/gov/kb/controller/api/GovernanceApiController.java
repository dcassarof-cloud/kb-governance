package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueAssignmentResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueAssignRequest;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueAssignResponsibleRequest;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueChangeStatusRequest;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueDetailResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueHistoryResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueIgnoreRequest;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceOverviewResponse;
import br.com.consisa.gov.kb.controller.api.dto.ResponsibleSummaryDto;
import br.com.consisa.gov.kb.controller.api.dto.SuggestedAssigneeResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueStatusResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueStatusUpdateRequest;
import br.com.consisa.gov.kb.controller.api.dto.PaginatedResponse;
import br.com.consisa.gov.kb.dto.DuplicateGroupDto;
import br.com.consisa.gov.kb.dto.GovernanceManualDto;
import br.com.consisa.gov.kb.dto.PageResponseDto;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.service.GovernanceService;
import br.com.consisa.gov.kb.service.GovernanceAssigneeService;
import br.com.consisa.gov.kb.service.GovernanceIssueWorkflowService;
import br.com.consisa.gov.kb.service.GovernanceOverviewService;
import br.com.consisa.gov.kb.service.GovernanceSlaService;
import br.com.consisa.gov.kb.service.IssueTypeMetaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static br.com.consisa.gov.kb.util.DateTimeUtils.toOffsetDateTime;
import static br.com.consisa.gov.kb.util.DateTimeUtils.toOffsetDateTimeOrNull;

/**
 * 🔍 Governance API Controller
 *
 * Endpoints:
 * - GET /api/v1/governance/issues (lista paginada)
 * - GET /api/v1/governance/duplicates (lista duplicados)
 */
@RestController
@RequestMapping("/api/v1/governance")
@CrossOrigin(origins = "*")
public class GovernanceApiController {

    private static final Logger log = LoggerFactory.getLogger(GovernanceApiController.class);

    private final KbGovernanceIssueRepository issueRepo;
    private final KbArticleRepository articleRepo;
    private final GovernanceService governanceService;
    private final GovernanceIssueWorkflowService workflowService;
    private final GovernanceAssigneeService assigneeService;
    private final GovernanceOverviewService overviewService;
    private final GovernanceSlaService slaService;
    private final IssueTypeMetaRegistry typeMetaRegistry;

    public GovernanceApiController(
            KbGovernanceIssueRepository issueRepo,
            KbArticleRepository articleRepo,
            GovernanceService governanceService,
            GovernanceIssueWorkflowService workflowService,
            GovernanceAssigneeService assigneeService,
            GovernanceOverviewService overviewService,
            GovernanceSlaService slaService,
            IssueTypeMetaRegistry typeMetaRegistry
    ) {
        this.issueRepo = issueRepo;
        this.articleRepo = articleRepo;
        this.governanceService = governanceService;
        this.workflowService = workflowService;
        this.assigneeService = assigneeService;
        this.overviewService = overviewService;
        this.slaService = slaService;
        this.typeMetaRegistry = typeMetaRegistry;
    }

    /**
     * GET /api/v1/governance?page=1&size=10
     * Alias para /issues (para compatibilidade)
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PaginatedResponse<GovernanceIssueResponse>> getGovernance(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/v1/governance (redirecting to /issues)");
        return getIssues(page, size, null, null, null, null, null, null, null, null, null, null, null);
    }

    // ========================================
    // SPRINT 5: Overview Gerencial
    // ========================================

    /**
     * GET /api/v1/governance/overview
     *
     * <p>Retorna overview gerencial com totais e métricas por sistema.
     *
     * @return overview com totals, bySystem e generatedAt
     */
    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public ResponseEntity<GovernanceOverviewResponse> getOverview() {
        log.info("GET /api/v1/governance/overview");

        GovernanceOverviewResponse overview = overviewService.generateOverview();

        log.info("✅ Overview gerado: open={}, critical={}, overdue={}, systems={}",
                overview.totals().open(),
                overview.totals().criticalOpen(),
                overview.totals().overdue(),
                overview.bySystem().size());

        return ResponseEntity.ok(overview);
    }

    /**
     * GET /api/v1/governance/issues?page=1&size=10&type=...&status=...
     *
     * 📋 LISTA PAGINADA DE ISSUES DE GOVERNANÇA COM FILTROS
     *
     * FILTROS SUPORTADOS (Sprint 5):
     * - type/issueType: INCOMPLETE_CONTENT, DUPLICATE_CONTENT, etc
     * - severity: ERROR, WARN, INFO
     * - status: OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, IGNORED
     * - systemCode: código do sistema
     * - responsibleType: USER, TEAM
     * - responsibleId: ID do responsável
     * - overdue: true para filtrar apenas vencidas
     * - unassigned: true para filtrar apenas sem responsável
     *
     * ORDENAÇÃO (Sprint 5):
     * - Vencidas primeiro (sla_due_at asc com overdue em cima)
     * - Depois severidade desc (ERROR > WARN > INFO)
     * - Depois created_at desc
     *
     * REGRAS:
     * - page é 1-based (converte para 0-based internamente)
     * - Retorna issues com dados do artigo, sistema e SLA enriquecidos
     * - Sem dados = lista vazia (não é erro)
     */
    @GetMapping("/issues")
    @Transactional(readOnly = true)
    public ResponseEntity<PaginatedResponse<GovernanceIssueResponse>> getIssues(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, name = "issueType") String issueType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String assigned,
            @RequestParam(required = false) String responsible,
            @RequestParam(required = false) String responsibleType,
            @RequestParam(required = false) String responsibleId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Boolean unassigned
    ) {
        log.info("GET /api/v1/governance/issues?page={}&size={}&type={}&status={}&systemCode={}&overdue={}&unassigned={}",
                page, size, type, status, systemCode, overdue, unassigned);

        // Converte page de 1-based para 0-based
        int pageIndex = Math.max(0, page - 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        var pageable = PageRequest.of(pageIndex, safeSize);

        // Normaliza filtros
        String rawType = (issueType != null && !issueType.isBlank()) ? issueType : type;
        String filterType = (rawType != null && !rawType.isBlank()) ? rawType : null;
        String filterSeverity = (severity != null && !severity.isBlank()) ? severity : null;
        String filterStatus = (status != null && !status.isBlank()) ? status : null;
        String filterSystemCode = (systemCode != null && !systemCode.isBlank()) ? systemCode : null;
        String filterResponsibleType = (responsibleType != null && !responsibleType.isBlank()) ? responsibleType : null;

        // Retrocompatibilidade: assigned/responsible pode ser responsibleId
        String rawResponsibleId = (responsibleId != null && !responsibleId.isBlank()) ? responsibleId : responsible;
        if (rawResponsibleId == null || rawResponsibleId.isBlank()) {
            rawResponsibleId = assigned;
        }
        String filterResponsibleId = (rawResponsibleId != null && !rawResponsibleId.isBlank()) ? rawResponsibleId : null;

        // Usa query avançada (Sprint 5)
        var pageResult = issueRepo.pageIssuesAdvanced(
                pageable,
                filterType,
                filterSeverity,
                filterStatus,
                filterSystemCode,
                filterResponsibleType,
                filterResponsibleId,
                overdue,
                unassigned
        );

        log.info("📊 Total de issues (filtros avançados): {}", pageResult.getTotalElements());

        // Mapeia para DTO com tratamento robusto
        List<GovernanceIssueResponse> items = pageResult.getContent().stream()
                .map(this::mapIssueDetailRowToDto)
                .collect(Collectors.toList());

        PaginatedResponse<GovernanceIssueResponse> response = new PaginatedResponse<>(
                page,  // retorna page original (1-based)
                safeSize,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                items
        );

        log.info("✅ Retornando {} issues (página {}/{})",
                items.size(), page, pageResult.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Mapeia IssueDetailRow (Sprint 5) para DTO de compatibilidade.
     */
    private GovernanceIssueResponse mapIssueDetailRowToDto(KbGovernanceIssueRepository.IssueDetailRow row) {
        return new GovernanceIssueResponse(
                row.getId(),
                row.getIssueType() != null ? row.getIssueType() : "UNKNOWN",
                row.getSeverity() != null ? row.getSeverity() : "WARN",
                row.getStatus() != null ? row.getStatus() : "OPEN",
                row.getArticleId(),
                row.getArticleTitle(),
                row.getSystemCode(),
                row.getSystemName(),
                row.getMessage(),
                toOffsetDateTime(row.getCreatedAt()),
                toOffsetDateTimeOrNull(row.getUpdatedAt()),
                row.getAssignedAgentId(),
                row.getAssignedAgentName(),
                toOffsetDateTimeOrNull(row.getDueDate()),
                row.getMessage()
        );
    }

    /**
     * POST /api/v1/governance/issues/{id}/assign
     */
    @PostMapping("/issues/{id}/assign")
    public ResponseEntity<GovernanceIssueAssignmentResponse> assignIssue(
            @PathVariable Long id,
            @RequestBody GovernanceIssueAssignRequest request
    ) {
        var assignment = workflowService.assignIssue(
                id,
                request.agentId(),
                request.agentName(),
                request.dueDate(),
                request.actor()
        );

        return ResponseEntity.ok(new GovernanceIssueAssignmentResponse(
                assignment.getId(),
                assignment.getIssueId(),
                assignment.getAgentId(),
                assignment.getAgentName(),
                assignment.getStatus().name(),
                assignment.getAssignedAt(),
                assignment.getDueDate()
        ));
    }

    /**
     * PATCH /api/v1/governance/issues/{id}/status
     */
    @PatchMapping("/issues/{id}/status")
    public ResponseEntity<GovernanceIssueStatusResponse> updateIssueStatus(
            @PathVariable Long id,
            @RequestBody GovernanceIssueStatusUpdateRequest request
    ) {
        if (request.status() == null || request.status().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Status é obrigatório");
        }
        GovernanceIssueStatus newStatus = parseStatus(request.status());
        if (newStatus == GovernanceIssueStatus.IGNORED) {
            throw new ResponseStatusException(BAD_REQUEST, "Use /ignore com motivo obrigatório.");
        }
        var issue = workflowService.updateStatus(id, newStatus, request.actor());
        return ResponseEntity.ok(new GovernanceIssueStatusResponse(issue.getId(), issue.getStatus().name()));
    }

    /**
     * POST /api/v1/governance/issues/{id}/status
     */
    @PostMapping("/issues/{id}/status")
    public ResponseEntity<GovernanceIssueStatusResponse> updateIssueStatusPost(
            @PathVariable Long id,
            @RequestBody GovernanceIssueStatusUpdateRequest request
    ) {
        return updateIssueStatus(id, request);
    }

    /**
     * POST /api/v1/governance/issues/{id}/ignore
     */
    @PostMapping("/issues/{id}/ignore")
    public ResponseEntity<GovernanceIssueStatusResponse> ignoreIssue(
            @PathVariable Long id,
            @RequestBody GovernanceIssueIgnoreRequest request
    ) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Motivo é obrigatório");
        }
        var issue = workflowService.ignoreIssue(id, request.reason(), request.actor());
        return ResponseEntity.ok(new GovernanceIssueStatusResponse(issue.getId(), issue.getStatus().name()));
    }

    // ========================================
    // SPRINT 5: Atribuição de Responsável Direto
    // ========================================

    /**
     * PUT /api/v1/governance/issues/{id}/assign
     *
     * <p>Atribui ou remove responsável direto de uma issue (Sprint 5).
     *
     * <p>Diferente do POST /assign legado que cria assignment separado,
     * este endpoint atualiza diretamente responsible_id e responsible_type na issue.
     *
     * @param id      ID da issue
     * @param request dados do responsável
     * @return issue atualizada
     */
    @PutMapping("/issues/{id}/assign")
    @Transactional
    public ResponseEntity<GovernanceIssueDetailResponse> assignResponsible(
            @PathVariable Long id,
            @RequestBody GovernanceIssueAssignResponsibleRequest request
    ) {
        log.info("PUT /api/v1/governance/issues/{}/assign - type={}, id={}",
                id, request.responsibleType(), request.responsibleId());

        if (!request.isValid()) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "responsibleType e responsibleId devem estar ambos preenchidos ou ambos vazios");
        }

        KbGovernanceIssue issue = issueRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Issue não encontrada: " + id));

        String oldResponsible = buildResponsibleString(issue.getResponsibleType(), issue.getResponsibleId());

        // Atualiza responsável
        issue.setResponsibleType(request.isUnassign() ? null : request.responsibleType());
        issue.setResponsibleId(request.isUnassign() ? null : request.responsibleId());

        // Se está atribuindo e status é OPEN, muda para ASSIGNED
        if (!request.isUnassign() && issue.getStatus() == GovernanceIssueStatus.OPEN) {
            issue.setStatus(GovernanceIssueStatus.ASSIGNED);
        }

        issueRepo.save(issue);

        // Registra histórico
        String newResponsible = buildResponsibleString(issue.getResponsibleType(), issue.getResponsibleId());
        String action = request.isUnassign() ? "UNASSIGNED" : "ASSIGNED";
        workflowService.recordHistoryEntry(id, action, oldResponsible, newResponsible, "system");

        log.info("✅ Responsável {} para issue {}: {} -> {}",
                request.isUnassign() ? "removido" : "atribuído",
                id, oldResponsible, newResponsible);

        return ResponseEntity.ok(mapIssueToDetailDto(issue));
    }

    /**
     * PUT /api/v1/governance/issues/{id}/status
     *
     * <p>Altera status de uma issue (Sprint 5).
     *
     * <p>Regras:
     * <ul>
     *   <li>IGNORED requer ignoredReason</li>
     *   <li>RESOLVED define resolvedAt = now</li>
     *   <li>RESOLVED → OPEN recalcula SLA (usa now como base)</li>
     *   <li>OPEN limpa resolvedAt</li>
     * </ul>
     *
     * @param id      ID da issue
     * @param request novo status e motivo (se IGNORED)
     * @return issue atualizada
     */
    @PutMapping("/issues/{id}/status")
    @Transactional
    public ResponseEntity<GovernanceIssueDetailResponse> changeStatus(
            @PathVariable Long id,
            @RequestBody GovernanceIssueChangeStatusRequest request
    ) {
        log.info("PUT /api/v1/governance/issues/{}/status - status={}", id, request.status());

        if (!request.isValid()) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Status inválido ou motivo obrigatório para IGNORED");
        }

        GovernanceIssueStatus newStatus;
        try {
            newStatus = GovernanceIssueStatus.valueOf(request.normalizedStatus());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Status inválido: " + request.status());
        }

        KbGovernanceIssue issue = issueRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Issue não encontrada: " + id));

        GovernanceIssueStatus oldStatus = issue.getStatus();

        if (oldStatus == newStatus) {
            return ResponseEntity.ok(mapIssueToDetailDto(issue));
        }

        // Atualiza status
        issue.setStatus(newStatus);

        // Regras de transição
        if (newStatus == GovernanceIssueStatus.RESOLVED) {
            issue.setResolvedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
            issue.setIgnoredReason(null);
        } else if (newStatus == GovernanceIssueStatus.IGNORED) {
            issue.setResolvedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
            issue.setIgnoredReason(request.ignoredReason());
        } else if (newStatus == GovernanceIssueStatus.OPEN) {
            // Reabertura: recalcula SLA usando now como base
            issue.setResolvedAt(null);
            issue.setIgnoredReason(null);
            if (oldStatus == GovernanceIssueStatus.RESOLVED || oldStatus == GovernanceIssueStatus.IGNORED) {
                issue.setSlaDueAt(slaService.calculateReopenedSlaDueAt(issue.getSeverity()));
            }
        } else {
            issue.setIgnoredReason(null);
        }

        issueRepo.save(issue);

        // Registra histórico
        workflowService.recordHistoryEntry(id, "STATUS_CHANGED", oldStatus.name(), newStatus.name(), "system");

        log.info("✅ Status alterado para issue {}: {} -> {}", id, oldStatus, newStatus);

        return ResponseEntity.ok(mapIssueToDetailDto(issue));
    }

    private String buildResponsibleString(String type, String id) {
        if (type == null && id == null) {
            return null;
        }
        return (type != null ? type : "") + ":" + (id != null ? id : "");
    }

    /**
     * GET /api/v1/governance/issues/{id}/history
     */
    @GetMapping("/issues/{id}/history")
    @Transactional(readOnly = true)
    public ResponseEntity<List<GovernanceIssueHistoryResponse>> getIssueHistory(@PathVariable Long id) {
        var history = workflowService.getHistory(id).stream()
                .map(item -> new GovernanceIssueHistoryResponse(
                        item.getId(),
                        item.getIssueId(),
                        item.getAction(),
                        item.getOldValue(),
                        item.getNewValue(),
                        item.getActor(),
                        item.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/v1/governance/manuals?page=1&size=10&system=CONSISANET&status=OK&q=texto
     *
     * 📋 Lista manuais/artigos para tela de governança.
     */
    @GetMapping("/manuals")
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponseDto<GovernanceManualDto>> getManuals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String system,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "q") String query
    ) {
        log.info("GET /api/v1/governance/manuals?page={}&size={}&system={}&status={}&q={}",
                page, size, system, status, query);

        PageResponseDto<GovernanceManualDto> response = governanceService.listManuals(page, size, system, status, query);

        log.info("✅ Manuais: totalItems={} totalPages={}", response.totalElements(), response.totalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/governance/duplicates
     *
     * Retorna grupos de artigos duplicados (mesmo content_hash).
     *
     * REGRAS:
     * - Sem duplicados = lista vazia (não é erro)
     * - Erro real = HTTP 500 (tratado pelo GlobalExceptionHandler)
     */
    @GetMapping("/duplicates")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DuplicateGroupDto>> getDuplicates() {
        log.info("GET /api/v1/governance/duplicates");

        List<DuplicateGroupDto> groups = governanceService.listDuplicates();

        log.info("✅ Retornando {} grupos de duplicados", groups.size());

        return ResponseEntity.ok(groups);
    }

    /**
     * GET /api/v1/governance/issues/{id}/suggested-assignee
     */
    @GetMapping("/issues/{id}/suggested-assignee")
    @Transactional(readOnly = true)
    public ResponseEntity<SuggestedAssigneeResponse> getSuggestedAssignee(@PathVariable Long id) {
        log.info("GET /api/v1/governance/issues/{}/suggested-assignee", id);
        return ResponseEntity.ok(assigneeService.suggestAssignee(id));
    }

    /**
     * GET /api/v1/governance/responsibles/summary
     */
    @GetMapping("/responsibles/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ResponsibleSummaryDto>> getResponsiblesSummary() {
        log.info("GET /api/v1/governance/responsibles/summary");
        return ResponseEntity.ok(assigneeService.listResponsiblesSummary());
    }

    // ======================
    // MAPPING
    // ======================

    /**
     * Mapeia resultado da query nativa IssueRow para DTO.
     * Já vem com artigo e sistema enriquecidos do JOIN.
     *
     * IMPORTANTE: PostgreSQL TIMESTAMPTZ → Instant → OffsetDateTime (UTC)
     * Usa DateTimeUtils.toOffsetDateTime() para conversão centralizada.
     */
    private GovernanceIssueResponse mapIssueRowToDto(KbGovernanceIssueRepository.IssueRow row) {
        String message = row.getMessage();
        return new GovernanceIssueResponse(
                row.getId(),
                row.getIssueType() != null ? row.getIssueType() : "UNKNOWN",
                row.getSeverity() != null ? row.getSeverity() : "WARN",
                row.getStatus() != null ? row.getStatus() : "OPEN",
                row.getArticleId(),
                row.getArticleTitle(),
                row.getSystemCode(),
                row.getSystemName(),
                message,
                toOffsetDateTime(row.getCreatedAt()),  // Instant → OffsetDateTime (UTC)
                toOffsetDateTimeOrNull(row.getUpdatedAt()),
                row.getAssignedAgentId(),
                row.getAssignedAgentName(),
                toOffsetDateTimeOrNull(row.getDueDate()),
                message
        );
    }

    /**
     * Mapeia entidade KbGovernanceIssue para DTO (fallback).
     * Usado quando a query nativa não está disponível.
     */
    private GovernanceIssueResponse mapIssueToDto(KbGovernanceIssue issue) {
        // Busca dados do artigo
        KbArticle article = articleRepo.findById(issue.getArticleId()).orElse(null);

        String articleTitle = null;
        String systemCode = null;
        String systemName = null;

        if (article != null) {
            articleTitle = article.getTitle();
            if (article.getSystem() != null) {
                systemCode = article.getSystem().getCode();
                systemName = article.getSystem().getName();
            }
        }

        // Converte evidence JSON para string (simplificado)
        String details = issue.getMessage();
        if (issue.getEvidence() != null) {
            details = issue.getMessage() + " | Evidence: " + issue.getEvidence().toString();
        }

        return new GovernanceIssueResponse(
                issue.getId(),
                issue.getIssueType().name(),
                issue.getSeverity().name(),
                issue.getStatus().name(),
                issue.getArticleId(),
                articleTitle,
                systemCode,
                systemName,
                details,
                issue.getCreatedAt(),
                issue.getUpdatedAt(),
                null,
                null,
                null,
                details
        );
    }

    private GovernanceIssueStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Status é obrigatório");
        }
        try {
            return GovernanceIssueStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Status inválido: " + status);
        }
    }

    /**
     * Mapeia entidade KbGovernanceIssue para DTO detalhado (Sprint 5).
     */
    private GovernanceIssueDetailResponse mapIssueToDetailDto(KbGovernanceIssue issue) {
        KbArticle article = articleRepo.findById(issue.getArticleId()).orElse(null);

        String articleTitle = null;
        String systemCode = null;
        String systemName = null;

        if (article != null) {
            articleTitle = article.getTitle();
            if (article.getSystem() != null) {
                systemCode = article.getSystem().getCode();
                systemName = article.getSystem().getName();
            }
        }

        // Metadados do tipo
        String typeDisplayName = typeMetaRegistry.getDisplayName(issue.getIssueType());
        String typeDescription = typeMetaRegistry.getDescription(issue.getIssueType());
        String typeRecommendation = typeMetaRegistry.getRecommendation(issue.getIssueType());

        // Verifica se está vencida
        boolean overdue = slaService.isOverdue(
                java.time.OffsetDateTime.now(),
                issue.getSlaDueAt(),
                issue.getStatus()
        );

        return new GovernanceIssueDetailResponse(
                issue.getId(),
                issue.getIssueType().name(),
                issue.getSeverity().name(),
                issue.getStatus().name(),
                issue.getArticleId(),
                articleTitle,
                systemCode,
                systemName,
                issue.getMessage(),
                issue.getCreatedAt(),
                issue.getUpdatedAt(),
                null, // assignedAgentId (legacy)
                null, // assignedAgentName (legacy)
                null, // dueDate (legacy)
                issue.getResponsibleId(),
                issue.getResponsibleType(),
                issue.getSlaDueAt(),
                issue.getResolvedAt(),
                issue.getIgnoredReason(),
                overdue,
                typeDisplayName,
                typeDescription,
                typeRecommendation,
                issue.getMessage()
        );
    }
}
