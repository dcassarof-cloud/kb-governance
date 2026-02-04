package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.ArticleListResponse;
import br.com.consisa.gov.kb.controller.api.dto.PaginatedResponse;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.service.GovernanceLanguageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 📄 Articles API Controller
 *
 * Endpoints:
 * - GET /api/v1/articles (lista paginada)
 * - GET /api/v1/articles/{id} (detalhe)
 */
@RestController
@RequestMapping("/api/v1/articles")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ANALYST')")
public class ArticlesApiController {

    private static final Logger log = LoggerFactory.getLogger(ArticlesApiController.class);

    private final KbArticleRepository articleRepo;
    private final GovernanceLanguageService languageService;

    public ArticlesApiController(KbArticleRepository articleRepo, GovernanceLanguageService languageService) {
        this.articleRepo = articleRepo;
        this.languageService = languageService;
    }

    /**
     * GET /api/v1/articles?page=1&size=10&q=...&systemCode=...&status=...
     *
     * IMPORTANTE: page recebido do front é 1-based.
     * Internamente converte para 0-based no PageRequest.
     */
    @GetMapping
    @Transactional(readOnly = true)  // ✅ FIX: Evita LazyInitializationException
    public ResponseEntity<PaginatedResponse<ArticleListResponse>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String status
    ) {
        log.info("GET /api/v1/articles?page={}&size={}&q={}&systemCode={}&status={}",
                page, size, q, systemCode, status);

        try {
            // Converte page de 1-based para 0-based
            int pageIndex = Math.max(0, page - 1);
            int safeSize = Math.max(1, Math.min(size, 100));

            Page<KbArticle> pageResult;

            // ✅ FIX: Se tem filtro de busca, busca TODOS os artigos e filtra
            if (q != null && !q.trim().isEmpty()) {
                String searchLower = q.toLowerCase().trim();
                log.info("🔍 Buscando por título: '{}'", q);

                // Busca TODOS os artigos (sem paginação inicial)
                List<KbArticle> allArticles = articleRepo.findAll(Sort.by(Sort.Direction.DESC, "updatedDate"));

                // Filtra por título
                List<KbArticle> filteredArticles = allArticles.stream()
                        .filter(a -> a.getTitle() != null && a.getTitle().toLowerCase().contains(searchLower))
                        .toList();

                log.info("📊 Encontrados {} artigos com '{}'", filteredArticles.size(), q);

                // Calcula paginação manual
                int totalFiltered = filteredArticles.size();
                int totalPages = (int) Math.ceil((double) totalFiltered / safeSize);
                int start = pageIndex * safeSize;
                int end = Math.min(start + safeSize, totalFiltered);

                List<KbArticle> pageArticles = start < totalFiltered
                        ? filteredArticles.subList(start, end)
                        : List.of();

                // Mapeia para DTO
                List<ArticleListResponse> items = pageArticles.stream()
                        .map(this::mapToDto)
                        .toList();

                PaginatedResponse<ArticleListResponse> response = new PaginatedResponse<>(
                        items,
                        page,
                        safeSize,
                        totalFiltered,  // total filtrado, não total geral
                        totalPages
                );

                log.info("✅ Retornando {} artigos filtrados (página {}/{})",
                        items.size(), page, totalPages);
                return ResponseEntity.ok(response);

            } else {
                // Sem filtros - busca paginada normal
                var pageable = PageRequest.of(pageIndex, safeSize, Sort.by(Sort.Direction.DESC, "updatedDate"));
                pageResult = articleRepo.findRecent(pageable);

                List<ArticleListResponse> items = pageResult.getContent().stream()
                        .map(this::mapToDto)
                        .toList();

                PaginatedResponse<ArticleListResponse> response = new PaginatedResponse<>(
                        items,
                        page,
                        safeSize,
                        pageResult.getTotalElements(),
                        pageResult.getTotalPages()
                );

                log.info("✅ Retornando {} artigos (página {}/{})",
                        items.size(), page, pageResult.getTotalPages());

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("❌ Erro ao buscar artigos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/v1/articles/{id}
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)  // ✅ FIX: Evita LazyInitializationException
    public ResponseEntity<ArticleListResponse> getArticleById(@PathVariable Long id) {
        log.info("GET /api/v1/articles/{}", id);

        return articleRepo.findById(id)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ======================
    // MAPPING
    // ======================

    private ArticleListResponse mapToDto(KbArticle article) {
        String systemCode = null;
        String systemName = null;

        if (article.getSystem() != null) {
            systemCode = article.getSystem().getCode();
            systemName = article.getSystem().getName();
        }

        return new ArticleListResponse(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getSourceUrl(),
                systemCode,
                systemName,
                languageService.governanceStatusLabel(article.getGovernanceStatus()),
                article.getUpdatedDate()
        );
    }
}
