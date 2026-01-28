package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.DashboardSummaryResponse;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.repository.KbSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 📊 Dashboard API Controller
 * 
 * Endpoint: GET /api/v1/dashboard/summary
 * 
 * Retorna estatísticas consolidadas para o dashboard do front.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "*")
public class DashboardApiController {

    private static final Logger log = LoggerFactory.getLogger(DashboardApiController.class);

    private final KbArticleRepository articleRepo;
    private final KbGovernanceIssueRepository issueRepo;
    private final KbSystemRepository systemRepo;

    public DashboardApiController(
            KbArticleRepository articleRepo,
            KbGovernanceIssueRepository issueRepo,
            KbSystemRepository systemRepo
    ) {
        this.articleRepo = articleRepo;
        this.issueRepo = issueRepo;
        this.systemRepo = systemRepo;
    }

    /**
     * GET /api/v1/dashboard/summary
     *
     * 📊 REGRAS DE NEGÓCIO (Sprint 1):
     *
     * - "Issues" = total de issues abertas (OPEN ou IN_PROGRESS)
     *   → Issue em tratamento (IN_PROGRESS) ainda é problema aberto
     *   → Só fecha quando status = RESOLVED
     *
     * - "OK" = total de artigos ATIVOS − artigos DISTINTOS com issue aberta
     *   → Um artigo com 3 issues abertas conta como 1 artigo com problema
     *   → OK nunca será 100% se houver issues abertas
     *
     * - "Duplicados" = quantidade de grupos de hashes duplicados
     *
     * IMPORTANTE:
     * - Erro real = HTTP 500 (tratado pelo GlobalExceptionHandler)
     * - Não mascara erro com dados zerados
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        log.info("GET /api/v1/dashboard/summary");

        // 1. Total de artigos ativos (article_status = 1)
        long totalArticles = articleRepo.countActiveArticles();

        // 2. Issues abertas (status = OPEN ou IN_PROGRESS)
        // REGRA: "Issue aberta" = OPEN ou IN_PROGRESS
        // Quando analista assume issue (IN_PROGRESS), continua sendo problema aberto
        long issuesCount = issueRepo.countOpenIssues();

        // 3. Artigos OK
        // REGRA: "OK" = total de artigos − artigos DISTINTOS com issue aberta
        // Um artigo com múltiplas issues conta só uma vez como "com problema"
        long articlesWithIssues = issueRepo.countDistinctArticlesWithOpenIssues();
        long okCount = Math.max(0, totalArticles - articlesWithIssues);

        // 4. Duplicados (quantidade de grupos de hashes duplicados)
        List<String> duplicateHashes = articleRepo.findDuplicateContentHashes();
        long duplicatesCount = duplicateHashes != null ? duplicateHashes.size() : 0L;

        // 5. Por sistema (sem dados = lista vazia, não é erro)
        List<Object[]> systemStats = articleRepo.countActiveBySystem();
        List<DashboardSummaryResponse.BySystem> bySystem = systemStats != null
                ? systemStats.stream()
                    .map(row -> new DashboardSummaryResponse.BySystem(
                            row[0] != null ? String.valueOf(row[0]) : "UNCLASSIFIED",
                            row[1] != null ? String.valueOf(row[1]) : "Não classificado",
                            row[2] != null ? ((Number) row[2]).longValue() : 0L
                    ))
                    .collect(Collectors.toList())
                : List.of();

        // 6. Por status (baseado em artigos distintos, não total de issues)
        List<DashboardSummaryResponse.ByStatus> byStatus = List.of(
                new DashboardSummaryResponse.ByStatus("OK", okCount),
                new DashboardSummaryResponse.ByStatus("WITH_ISSUES", articlesWithIssues)
        );

        DashboardSummaryResponse response = new DashboardSummaryResponse(
                totalArticles,
                okCount,
                issuesCount,
                duplicatesCount,
                bySystem,
                byStatus
        );

        log.info("✅ Dashboard: total={} OK={} issues={} withIssues={} duplicates={}",
                totalArticles, okCount, issuesCount, articlesWithIssues, duplicatesCount);

        return ResponseEntity.ok(response);
    }
}
