package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueAssignmentResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueAssignResponsibleRequest;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueAssignRequest;
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
import br.com.consisa.gov.kb.domain.GovernanceResponsibleType;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.service.GovernanceService;
import br.com.consisa.gov.kb.service.GovernanceAssigneeService;
import br.com.consisa.gov.kb.service.GovernanceIssueWorkflowService;
import br.com.consisa.gov.kb.service.GovernanceOverviewService;
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
    private final IssueTypeMetaRegistry issueTypeMetaRegistry;

    public GovernanceApiController(
            KbGovernanceIssueRepository issueRepo,
            KbArticleRepository articleRepo,
            GovernanceService governanceService,
            GovernanceIssueWorkflowService workflowService,
            GovernanceAssigneeService assigneeService,
            GovernanceOverviewService overviewService,
            IssueTypeMetaRegistry issueTypeMetaRegistry
    ) {
        this.issueRepo = issueRepo;
        this.articleRepo = articleRepo;
        this.governanceService = governanceService;
        this.workflowService = workflowService;
        this.assigneeService = assigneeService;
        this.overviewService = overviewService;
        this.issueTypeMetaRegistry = issueTypeMetaRegistry;
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
        return getIssues(
                page,
                size,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * GET /api/v1/governance/issues?page=1&size=10&type=...&status=...
     *
     * 📋 LISTA PAGINADA DE ISSUES DE GOVERNANÇA COM FILTROS
     *
     * FILTROS SUPORTADOS (Sprint 2):
     * - type: INCOMPLETE_CONTENT, DUPLICATE_CONTENT, OUTDATED_CONTENT, INCONSISTENT_CONTENT
     * - status: OPEN, IN_PROGRESS, RESOLVED
     *
     * REGRAS:
     * - page é 1-based (converte para 0-based internamente)
     * - Retorna issues com dados do artigo e sistema enriquecidos
     * - Sem dados = lista vazia (não é erro)
     * - Erro real = HTTP 500 (tratado pelo GlobalExceptionHandler)
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
        log.info("GET /api/v1/governance/issues?page={}&size={}&type={}&issueType={}&status={}&systemCode={}&assigned={}",
                page, size, type, issueType, status, systemCode, assigned);

        // Converte page de 1-based para 0-based
        int pageIndex = Math.max(0, page - 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        var pageable = PageRequest.of(pageIndex, safeSize);

        // Usa query com filtros se type ou status foram informados
        // Passa null para filtros vazios ou em branco
        String rawType = (issueType != null && !issueType.isBlank()) ? issueType : type;
        String filterType = (rawType != null && !rawType.isBlank()) ? rawType : null;
        String filterSeverity = (severity != null && !severity.isBlank()) ? severity : null;
        String filterStatus = (status != null && !status.isBlank()) ? status : null;
        String filterSystemCode = (systemCode != null && !systemCode.isBlank()) ? systemCode : null;
        String rawAssigned = (assigned != null && !assigned.isBlank()) ? assigned : responsible;
        String filterResponsible = (responsibleId != null && !responsibleId.isBlank())
                ? responsibleId
                : ((rawAssigned != null && !rawAssigned.isBlank()) ? rawAssigned : null);
        String filterResponsibleType = (responsibleType != null && !responsibleType.isBlank()) ? responsibleType : null;

        var pageResult = (filterType != null || filterStatus != null || filterSeverity != null
                || filterSystemCode != null || filterResponsible != null || filterResponsibleType != null
                || Boolean.TRUE.equals(overdue) || Boolean.TRUE.equals(unassigned))
                ? issueRepo.pageIssuesFiltered(pageable, filterType, filterSeverity, filterStatus, filterSystemCode,
                filterResponsible, filterResponsibleType, overdue, unassigned)
                : issueRepo.pageIssues(pageable);

        log.info("📊 Total de issues (filtros: type={}, status={}, system={}, assigned={}): {}",
                filterType, filterStatus, filterSystemCode, filterResponsible, pageResult.getTotalElements());

        // Mapeia para DTO com tratamento robusto
        List<GovernanceIssueResponse> items = pageResult.getContent().stream()
                .map(this::mapIssueRowToDto)
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
     * PUT /api/v1/governance/issues/{id}/assign
     */
    @PutMapping("/issues/{id}/assign")
    public ResponseEntity<GovernanceIssueAssignmentResponse> assignIssueV2(
            @PathVariable Long id,
            @RequestBody GovernanceIssueAssignResponsibleRequest request
    ) {
        GovernanceResponsibleType type = parseResponsibleType(request.responsibleType());
        var assignment = workflowService.assignResponsible(
                id,
                type,
                request.responsibleId(),
                request.responsibleName(),
                null,
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
     * POST /api/v1/governance/issues/{id}/assign
     */
    @Deprecated
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
     * PUT /api/v1/governance/issues/{id}/status
     */
    @PutMapping("/issues/{id}/status")
    public ResponseEntity<GovernanceIssueStatusResponse> updateIssueStatusV2(
            @PathVariable Long id,
            @RequestBody GovernanceIssueStatusUpdateRequest request
    ) {
        if (request.status() == null || request.status().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Status é obrigatório");
        }
        GovernanceIssueStatus newStatus = parseStatus(request.status());
        if (newStatus == GovernanceIssueStatus.IGNORED
                && (request.ignoredReason() == null || request.ignoredReason().isBlank())) {
            throw new ResponseStatusException(BAD_REQUEST, "Motivo é obrigatório para IGNORED.");
        }
        var issue = workflowService.updateStatus(id, newStatus, request.actor(), request.ignoredReason());
        return ResponseEntity.ok(new GovernanceIssueStatusResponse(issue.getId(), issue.getStatus().name()));
    }

    /**
     * PATCH /api/v1/governance/issues/{id}/status
     */
    @Deprecated
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
        var issue = workflowService.updateStatus(id, newStatus, request.actor(), null);
        return ResponseEntity.ok(new GovernanceIssueStatusResponse(issue.getId(), issue.getStatus().name()));
    }

    /**
     * POST /api/v1/governance/issues/{id}/status
     */
    @Deprecated
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
     * GET /api/v1/governance/overview
     */
    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public ResponseEntity<GovernanceOverviewResponse> getOverview() {
        return ResponseEntity.ok(overviewService.getOverview());
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
        var meta = resolveIssueTypeMeta(row.getIssueType());
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
                message,
                row.getResponsibleId(),
                row.getResponsibleType(),
                toOffsetDateTimeOrNull(row.getSlaDueAt()),
                toOffsetDateTimeOrNull(row.getResolvedAt()),
                row.getIgnoredReason(),
                meta != null ? meta.displayName() : null,
                meta != null ? meta.description() : null,
                meta != null ? meta.recommendation() : null
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

        var meta = issueTypeMetaRegistry.getMeta(issue.getIssueType());
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
                details,
                issue.getResponsibleId(),
                issue.getResponsibleType() != null ? issue.getResponsibleType().name() : null,
                issue.getSlaDueAt(),
                issue.getResolvedAt(),
                issue.getIgnoredReason(),
                meta != null ? meta.displayName() : null,
                meta != null ? meta.description() : null,
                meta != null ? meta.recommendation() : null
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

    private GovernanceResponsibleType parseResponsibleType(String responsibleType) {
        if (responsibleType == null || responsibleType.isBlank()) {
            return GovernanceResponsibleType.USER;
        }
        try {
            return GovernanceResponsibleType.valueOf(responsibleType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Tipo de responsável inválido: " + responsibleType);
        }
    }

    private IssueTypeMetaRegistry.IssueTypeMeta resolveIssueTypeMeta(String issueType) {
        if (issueType == null || issueType.isBlank()) {
            return null;
        }
        try {
            KbGovernanceIssueType type = KbGovernanceIssueType.valueOf(issueType);
            return issueTypeMetaRegistry.getMeta(type);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
