package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.DashboardSummaryResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceDashboardResponse;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.repository.KbSystemRepository;
import br.com.consisa.gov.kb.service.GovernanceDashboardService;
import br.com.consisa.gov.kb.service.GovernanceLanguageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class DashboardApiController {

    private static final Logger log = LoggerFactory.getLogger(DashboardApiController.class);

    private final KbArticleRepository articleRepo;
    private final KbGovernanceIssueRepository issueRepo;
    private final KbSystemRepository systemRepo;
    private final GovernanceLanguageService languageService;
    private final GovernanceDashboardService governanceDashboardService;

    public DashboardApiController(
            KbArticleRepository articleRepo,
            KbGovernanceIssueRepository issueRepo,
            KbSystemRepository systemRepo,
            GovernanceLanguageService languageService,
            GovernanceDashboardService governanceDashboardService
    ) {
        this.articleRepo = articleRepo;
        this.issueRepo = issueRepo;
        this.systemRepo = systemRepo;
        this.languageService = languageService;
        this.governanceDashboardService = governanceDashboardService;
    }

    /**
     * GET /api/v1/dashboard/summary
     *
     * 📊 REGRAS DE NEGÓCIO (Sprint 1):
     *
     * - "Total Articles" = COUNT(*) em kb_article
     * - "Articles With Issues" = COUNT(DISTINCT article_id) em kb_governance_issue
     * - "Articles OK" = totalArticles - articlesWithIssues
     * - "Total Issues" = COUNT(*) em kb_governance_issue (informativo)
     *
     * - "Duplicados" = quantidade de grupos de hashes duplicados (informativo)
     *
     * IMPORTANTE:
     * - Erro real = HTTP 500 (tratado pelo GlobalExceptionHandler)
     * - Não mascara erro com dados zerados
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        log.info("GET /api/v1/dashboard/summary");

        // 1. Total de artigos (sem filtro de status)
        long totalArticles = articleRepo.count();

        // 2. Artigos com issues (distinct article_id em kb_governance_issue)
        long articlesWithIssues = issueRepo.countDistinctArticlesWithIssues();

        // 3. Artigos OK
        long articlesOk = Math.max(0, totalArticles - articlesWithIssues);

        // 4. Total de issues (informativo)
        long totalIssues = issueRepo.countTotalIssues();

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
                new DashboardSummaryResponse.ByStatus(languageService.governanceStatusLabel("OK"), articlesOk),
                new DashboardSummaryResponse.ByStatus(languageService.governanceStatusLabel("WITH_ISSUES"), articlesWithIssues)
        );

        DashboardSummaryResponse response = new DashboardSummaryResponse(
                totalArticles,
                articlesOk,
                articlesWithIssues,
                totalIssues,
                duplicatesCount,
                bySystem,
                byStatus
        );

        log.info("✅ Dashboard: total={} OK={} withIssues={} totalIssues={} duplicates={}",
                totalArticles, articlesOk, articlesWithIssues, totalIssues, duplicatesCount);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/dashboard/governance
     *
     * Dashboard de governança para tomada de decisão.
     */
    @GetMapping("/governance")
    public ResponseEntity<GovernanceDashboardResponse> getGovernanceDashboard() {
        log.info("GET /api/v1/dashboard/governance");
        return ResponseEntity.ok(governanceDashboardService.getGovernanceDashboard());
    }
}
