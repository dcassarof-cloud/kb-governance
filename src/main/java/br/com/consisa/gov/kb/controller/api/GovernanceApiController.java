package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.DuplicateGroupResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueResponse;
import br.com.consisa.gov.kb.controller.api.dto.PaginatedResponse;
import br.com.consisa.gov.kb.dto.GovernanceManualDto;
import br.com.consisa.gov.kb.dto.PageResponseDto;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.service.GovernanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static br.com.consisa.gov.kb.util.DateTimeUtils.toOffsetDateTime;

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

    public GovernanceApiController(
            KbGovernanceIssueRepository issueRepo,
            KbArticleRepository articleRepo,
            GovernanceService governanceService
    ) {
        this.issueRepo = issueRepo;
        this.articleRepo = articleRepo;
        this.governanceService = governanceService;
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
        return getIssues(page, size, null, null, null, null);
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
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String systemCode
    ) {
        log.info("GET /api/v1/governance/issues?page={}&size={}&type={}&status={}", page, size, type, status);

        // Converte page de 1-based para 0-based
        int pageIndex = Math.max(0, page - 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        var pageable = PageRequest.of(pageIndex, safeSize);

        // Usa query com filtros se type ou status foram informados
        // Passa null para filtros vazios ou em branco
        String filterType = (type != null && !type.isBlank()) ? type : null;
        String filterStatus = (status != null && !status.isBlank()) ? status : null;

        var pageResult = (filterType != null || filterStatus != null)
                ? issueRepo.pageIssuesFiltered(pageable, filterType, filterStatus)
                : issueRepo.pageIssues(pageable);

        log.info("📊 Total de issues (filtros: type={}, status={}): {}",
                filterType, filterStatus, pageResult.getTotalElements());

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
    public ResponseEntity<List<DuplicateGroupResponse>> getDuplicates() {
        log.info("GET /api/v1/governance/duplicates");

        // 1. Busca hashes duplicados (com proteção contra null)
        List<String> duplicateHashes = articleRepo.findDuplicateContentHashes();
        if (duplicateHashes == null || duplicateHashes.isEmpty()) {
            log.info("✅ Nenhum grupo de duplicados encontrado");
            return ResponseEntity.ok(List.of());
        }

        // 2. Para cada hash, busca os artigos
        List<DuplicateGroupResponse> groups = new ArrayList<>();

        for (String hash : duplicateHashes) {
            if (hash == null || hash.isBlank()) continue;

            List<Long> articleIds = articleRepo.findArticleIdsByContentHash(hash);

            if (articleIds != null && articleIds.size() > 1) {
                groups.add(new DuplicateGroupResponse(
                        hash,
                        articleIds.size(),
                        articleIds
                ));
            }
        }

        log.info("✅ Retornando {} grupos de duplicados", groups.size());

        return ResponseEntity.ok(groups);
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
                toOffsetDateTime(row.getCreatedAt())  // Instant → OffsetDateTime (UTC)
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
                issue.getCreatedAt()
        );
    }
}
