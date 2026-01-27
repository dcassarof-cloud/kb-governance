package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.DuplicateGroupResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceIssueResponse;
import br.com.consisa.gov.kb.controller.api.dto.PaginatedResponse;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public GovernanceApiController(
            KbGovernanceIssueRepository issueRepo,
            KbArticleRepository articleRepo
    ) {
        this.issueRepo = issueRepo;
        this.articleRepo = articleRepo;
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
     * GET /api/v1/governance/issues?page=1&size=10&type=...&severity=...&status=...
     *
     * IMPORTANTE: page é 1-based (converte para 0-based internamente)
     */
    @GetMapping("/issues")
    @Transactional(readOnly = true)  // ✅ FIX: Evita LazyInitializationException
    public ResponseEntity<PaginatedResponse<GovernanceIssueResponse>> getIssues(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String systemCode
    ) {
        log.info("GET /api/v1/governance/issues?page={}&size={}", page, size);

        try {
            // Converte page de 1-based para 0-based
            int pageIndex = Math.max(0, page - 1);
            int safeSize = Math.max(1, Math.min(size, 100));

            // Busca issues (simplificado - sem filtros ainda)
            var pageable = PageRequest.of(pageIndex, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            var pageResult = issueRepo.findAll(pageable);

            log.info("📊 Total de issues no banco: {}", pageResult.getTotalElements());

            // Mapeia para DTO
            List<GovernanceIssueResponse> items = pageResult.getContent().stream()
                    .map(this::mapIssueToDto)
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

        } catch (Exception e) {
            log.error("❌ Erro ao buscar issues: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/v1/governance/duplicates
     *
     * Retorna grupos de artigos duplicados (mesmo content_hash).
     */
    @GetMapping("/duplicates")
    @Transactional(readOnly = true)  // ✅ FIX: Evita LazyInitializationException
    public ResponseEntity<List<DuplicateGroupResponse>> getDuplicates() {
        log.info("GET /api/v1/governance/duplicates");

        try {
            // 1. Busca hashes duplicados
            List<String> duplicateHashes = articleRepo.findDuplicateContentHashes();

            // 2. Para cada hash, busca os artigos
            List<DuplicateGroupResponse> groups = new ArrayList<>();

            for (String hash : duplicateHashes) {
                List<Long> articleIds = articleRepo.findArticleIdsByContentHash(hash);

                if (articleIds.size() > 1) {
                    groups.add(new DuplicateGroupResponse(
                            hash,
                            articleIds.size(),
                            articleIds
                    ));
                }
            }

            log.info("✅ Retornando {} grupos de duplicados", groups.size());

            return ResponseEntity.ok(groups);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar duplicados: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ======================
    // MAPPING
    // ======================

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