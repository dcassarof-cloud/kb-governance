package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueAssignResponsibleRequest;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueAssignRequest;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueHistoryItemResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueHistoryListResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueIgnoreRequest;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueStatusResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueStatusUpdateRequest;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceOverviewResponse;
import br.com.consisa.gov.kb.controller.api.dto.PaginatedResponse;
import br.com.consisa.gov.kb.controller.api.dto.ResponsibleSummaryDto;
import br.com.consisa.gov.kb.controller.api.dto.SuggestedAssigneeResponse;
import br.com.consisa.gov.kb.dto.DuplicateGroupDto;
import br.com.consisa.gov.kb.dto.GovernanceManualDto;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceResponsibleType;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.repository.KbSystemRepository;
import br.com.consisa.gov.kb.repository.AppUserRepository;
import br.com.consisa.gov.kb.security.SecurityUtils;
import br.com.consisa.gov.kb.service.GovernanceService;
import br.com.consisa.gov.kb.service.GovernanceAssigneeService;
import br.com.consisa.gov.kb.service.GovernanceLanguageService;
import br.com.consisa.gov.kb.service.GovernanceIssueWorkflowService;
import br.com.consisa.gov.kb.service.GovernanceOverviewService;
import br.com.consisa.gov.kb.service.IssueTypeMetaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST','AGENT')")
public class GovernanceApiController {

    private static final Logger log = LoggerFactory.getLogger(GovernanceApiController.class);

    private final KbGovernanceIssueRepository issueRepo;
    private final KbArticleRepository articleRepo;
    private final GovernanceService governanceService;
    private final GovernanceIssueWorkflowService workflowService;
    private final GovernanceAssigneeService assigneeService;
    private final GovernanceOverviewService overviewService;
    private final IssueTypeMetaRegistry issueTypeMetaRegistry;
    private final GovernanceLanguageService languageService;
    private final KbSystemRepository systemRepository;
    private final AppUserRepository userRepository;

    public GovernanceApiController(
            KbGovernanceIssueRepository issueRepo,
            KbArticleRepository articleRepo,
            GovernanceService governanceService,
            GovernanceIssueWorkflowService workflowService,
            GovernanceAssigneeService assigneeService,
            GovernanceOverviewService overviewService,
            IssueTypeMetaRegistry issueTypeMetaRegistry,
            GovernanceLanguageService languageService,
            KbSystemRepository systemRepository,
            AppUserRepository userRepository
    ) {
        this.issueRepo = issueRepo;
        this.articleRepo = articleRepo;
        this.governanceService = governanceService;
        this.workflowService = workflowService;
        this.assigneeService = assigneeService;
        this.overviewService = overviewService;
        this.issueTypeMetaRegistry = issueTypeMetaRegistry;
        this.languageService = languageService;
        this.systemRepository = systemRepository;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/v1/governance?page=1&size=10
     * Alias para /issues (para compatibilidade)
     */
    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST','AGENT')")
    public ResponseEntity<PaginatedResponse<GovernanceIssueResponse>> getGovernance(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
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
                null,
                null,
                request
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST','AGENT')")
    public ResponseEntity<PaginatedResponse<GovernanceIssueResponse>> getIssues(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, name = "issueType") String issueType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) String assigned,
            @RequestParam(required = false) String responsible,
            @RequestParam(required = false) String responsibleType,
            @RequestParam(required = false) String responsibleId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Boolean unassigned,
            HttpServletRequest request
    ) {
        String requestId = resolveRequestId(request);
        log.info("GET /api/v1/governance/issues requestId={} page={} size={} type={} issueType={} status={} systemCode={} assigned={}",
                requestId, page, size, type, issueType, status, systemCode, assigned);

        // Converte page de 1-based para 0-based
        int pageIndex = Math.max(0, page - 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        var pageable = PageRequest.of(pageIndex, safeSize);

        // Usa query com filtros se type ou status foram informados
        // Passa null para filtros vazios ou em branco
        String rawType = (issueType != null && !issueType.isBlank()) ? issueType : type;
        String filterType = parseIssueTypeFilter(rawType);
        String filterSeverity = parseSeverityFilter(severity);
        String filterStatus = parseStatusFilter(status);
        String filterSystemCode = normalizeSystemCode(systemCode);
        String rawAssigned = (assigned != null && !assigned.isBlank()) ? assigned : responsible;
        String filterResponsible = (responsibleId != null && !responsibleId.isBlank())
                ? responsibleId
                : ((rawAssigned != null && !rawAssigned.isBlank()) ? rawAssigned : null);
        String filterResponsibleType = (responsibleType != null && !responsibleType.isBlank()) ? responsibleType : null;
        String filterQuery = (query != null && !query.isBlank()) ? query.trim() : null;

        if (filterSystemCode != null && systemRepository.findByCode(filterSystemCode).isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "systemCode inválido");
        }

        if (isAgent()) {
            filterResponsible = resolveAgentId();
        }

        var pageResult = (filterType != null || filterStatus != null || filterSeverity != null
                || filterSystemCode != null || filterResponsible != null || filterResponsibleType != null
                || filterQuery != null || Boolean.TRUE.equals(overdue) || Boolean.TRUE.equals(unassigned))
                ? issueRepo.pageIssuesFiltered(pageable, filterType, filterSeverity, filterStatus, filterSystemCode,
                filterResponsible, filterResponsibleType, filterQuery, overdue, unassigned)
                : issueRepo.pageIssues(pageable);

        log.info("📊 Total de issues (filtros: type={}, status={}, system={}, assigned={}): {}",
                filterType, filterStatus, filterSystemCode, filterResponsible, pageResult.getTotalElements());

        // Mapeia para DTO com tratamento robusto
        var mappedPage = pageResult.map(this::mapIssueRowToDto);
        PaginatedResponse<GovernanceIssueResponse> response = PaginatedResponse.from(mappedPage, page, safeSize);

        log.info("✅ Retornando {} issues (página {}/{})",
                mappedPage.getNumberOfElements(), page, pageResult.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/governance/issues/{id}/assign
     */
    @PutMapping("/issues/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST')")
    public ResponseEntity<GovernanceIssueResponse> assignIssueV2(
            @PathVariable Long id,
            @RequestBody GovernanceIssueAssignResponsibleRequest request
    ) {
        GovernanceResponsibleType type = parseResponsibleType(request.responsibleType());
        workflowService.assignResponsible(
                id,
                type,
                request.responsibleId(),
                request.responsibleName(),
                null,
                request.actor()
        );

        return ResponseEntity.ok(getIssueResponse(id));
    }

    /**
     * POST /api/v1/governance/issues/{id}/assign
     */
    @Deprecated
    @PostMapping("/issues/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST')")
    public ResponseEntity<GovernanceIssueResponse> assignIssue(
            @PathVariable Long id,
            @RequestBody GovernanceIssueAssignRequest request
    ) {
        workflowService.assignIssue(
                id,
                request.agentId(),
                request.agentName(),
                request.dueDate(),
                request.actor()
        );

        return ResponseEntity.ok(getIssueResponse(id));
    }

    /**
     * PUT /api/v1/governance/issues/{id}/status
     */
    @PutMapping("/issues/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST') or (hasRole('AGENT') and @issueAccessService.isAssignedToCurrentUser(#id))")
    public ResponseEntity<GovernanceIssueResponse> updateIssueStatusV2(
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
        workflowService.updateStatus(id, newStatus, request.actor(), request.ignoredReason());
        return ResponseEntity.ok(getIssueResponse(id));
    }

    /**
     * PATCH /api/v1/governance/issues/{id}/status
     */
    @Deprecated
    @PatchMapping("/issues/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST') or (hasRole('AGENT') and @issueAccessService.isAssignedToCurrentUser(#id))")
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
        return ResponseEntity.ok(new GovernanceIssueStatusResponse(
                issue.getId(),
                languageService.issueStatusLabel(issue.getStatus())
        ));
    }

    /**
     * POST /api/v1/governance/issues/{id}/status
     */
    @Deprecated
    @PostMapping("/issues/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST') or (hasRole('AGENT') and @issueAccessService.isAssignedToCurrentUser(#id))")
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST') or (hasRole('AGENT') and @issueAccessService.isAssignedToCurrentUser(#id))")
    public ResponseEntity<GovernanceIssueStatusResponse> ignoreIssue(
            @PathVariable Long id,
            @RequestBody GovernanceIssueIgnoreRequest request
    ) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Motivo é obrigatório");
        }
        var issue = workflowService.ignoreIssue(id, request.reason(), request.actor());
        return ResponseEntity.ok(new GovernanceIssueStatusResponse(
                issue.getId(),
                languageService.issueStatusLabel(issue.getStatus())
        ));
    }

    /**
     * GET /api/v1/governance/issues/{id}/history
     */
    @GetMapping("/issues/{id}/history")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST') or (hasRole('AGENT') and @issueAccessService.isAssignedToCurrentUser(#id))")
    public ResponseEntity<GovernanceIssueHistoryListResponse> getIssueHistory(@PathVariable Long id) {
        var history = workflowService.getHistory(id).stream()
                .map(item -> new GovernanceIssueHistoryItemResponse(
                        item.getAction(),
                        item.getActor(),
                        item.getCreatedAt(),
                        item.getOldValue(),
                        item.getNewValue()
                ))
                .toList();

        return ResponseEntity.ok(new GovernanceIssueHistoryListResponse(history));
    }

    /**
     * GET /api/v1/governance/issues/{id}
     */
    @GetMapping("/issues/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST') or (hasRole('AGENT') and @issueAccessService.isAssignedToCurrentUser(#id))")
    public ResponseEntity<GovernanceIssueResponse> getIssue(@PathVariable Long id) {
        return ResponseEntity.ok(getIssueResponse(id));
    }

    /**
     * GET /api/v1/governance/overview
     */
    @GetMapping("/overview")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST')")
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
    public ResponseEntity<PaginatedResponse<GovernanceManualDto>> getManuals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String system,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "q") String query
    ) {
        log.info("GET /api/v1/governance/manuals?page={}&size={}&system={}&status={}&q={}",
                page, size, system, status, query);

        var response = governanceService.listManuals(page, size, system, status, query);
        PaginatedResponse<GovernanceManualDto> paginatedResponse = new PaginatedResponse<>(
                response.items(),
                response.page(),
                response.size(),
                response.totalElements(),
                response.totalPages()
        );

        log.info("✅ Manuais: totalItems={} totalPages={}", response.totalElements(), response.totalPages());

        return ResponseEntity.ok(paginatedResponse);
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
        var meta = resolveIssueTypeMeta(row.getIssueType());
        OffsetDateTime slaDueAt = toOffsetDateTimeOrNull(row.getSlaDueAt());
        return new GovernanceIssueResponse(
                row.getId(),
                row.getIssueType(),
                meta != null ? meta.displayName() : null,
                meta != null ? meta.description() : null,
                meta != null ? meta.recommendation() : null,
                row.getSystemCode(),
                row.getSystemName(),
                row.getStatus(),
                row.getSeverity(),
                row.getResponsibleType(),
                row.getResponsibleId(),
                slaDueAt,
                isOverdue(row.getStatus(), slaDueAt),
                toOffsetDateTime(row.getCreatedAt()),
                toOffsetDateTimeOrNull(row.getUpdatedAt())
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

        var meta = issueTypeMetaRegistry.getMeta(issue.getIssueType());
        OffsetDateTime slaDueAt = issue.getSlaDueAt();
        return new GovernanceIssueResponse(
                issue.getId(),
                issue.getIssueType().name(),
                meta != null ? meta.displayName() : null,
                meta != null ? meta.description() : null,
                meta != null ? meta.recommendation() : null,
                systemCode,
                systemName,
                issue.getStatus().name(),
                issue.getSeverity().name(),
                issue.getResponsibleType() != null ? issue.getResponsibleType().name() : null,
                issue.getResponsibleId(),
                slaDueAt,
                isOverdue(issue.getStatus().name(), slaDueAt),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }

    private GovernanceIssueResponse getIssueResponse(Long id) {
        return issueRepo.findIssueRowById(id)
                .map(this::mapIssueRowToDto)
                .orElseGet(() -> {
                    KbGovernanceIssue issue = issueRepo.findById(id)
                            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Issue não encontrada: " + id));
                    return mapIssueToDto(issue);
                });
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

    private String parseStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return GovernanceIssueStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Status inválido: " + status);
        }
    }

    private String parseIssueTypeFilter(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return KbGovernanceIssueType.valueOf(type.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Tipo de issue inválido: " + type);
        }
    }

    private String parseSeverityFilter(String severity) {
        if (severity == null || severity.isBlank()) {
            return null;
        }
        try {
            return GovernanceSeverity.valueOf(severity.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Severidade inválida: " + severity);
        }
    }

    private String normalizeSystemCode(String systemCode) {
        if (systemCode == null || systemCode.isBlank()) {
            return null;
        }
        return systemCode.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isAgent() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private String resolveAgentId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(user -> user.getAgentId())
                .orElse(null);
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

    private boolean isOverdue(String status, OffsetDateTime slaDueAt) {
        if (slaDueAt == null) {
            return false;
        }
        if (status == null) {
            return false;
        }
        String normalized = status.toUpperCase();
        if ("RESOLVED".equals(normalized) || "IGNORED".equals(normalized)) {
            return false;
        }
        return slaDueAt.isBefore(OffsetDateTime.now());
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

    private String resolveRequestId(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String requestId = request.getHeader("x-request-id");
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader("x-correlation-id");
        }
        return requestId != null ? requestId : "unknown";
    }
}
