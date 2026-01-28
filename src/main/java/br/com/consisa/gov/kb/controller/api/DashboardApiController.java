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
     * Retorna resumo do dashboard com:
     * - Total de artigos
     * - Artigos OK (sem issues abertas)
     * - Issues abertas
     * - Duplicados detectados
     * - Distribuição por sistema
     * - Distribuição por status
     */
    /**
     * GET /api/v1/dashboard/summary
     *
     * 📊 REGRAS DE NEGÓCIO (Sprint 1):
     * - "Issues" = total de issues abertas (status = OPEN)
     * - "OK" = total de artigos ATIVOS − artigos DISTINTOS com issue aberta
     * - "Duplicados" = quantidade de grupos de hashes duplicados
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        log.info("GET /api/v1/dashboard/summary");

        try {
            // 1. Total de artigos ativos (article_status = 1)
            long totalArticles = articleRepo.countActiveArticles();

            // 2. Issues abertas (status = OPEN)
            // REGRA: "Issues" = issues abertas (OPEN)
            long issuesCount = issueRepo.countOpenIssues();

            // 3. Artigos OK
            // REGRA: "OK" = total de artigos − artigos DISTINTOS com issue aberta
            long articlesWithIssues = issueRepo.countDistinctArticlesWithOpenIssues();
            long okCount = Math.max(0, totalArticles - articlesWithIssues);

            // 4. Duplicados (quantidade de grupos de hashes duplicados)
            List<String> duplicateHashes = articleRepo.findDuplicateContentHashes();
            long duplicatesCount = duplicateHashes != null ? duplicateHashes.size() : 0L;

            // 5. Por sistema (usando query nativa com tratamento robusto)
            List<DashboardSummaryResponse.BySystem> bySystem;
            try {
                List<Object[]> systemStats = articleRepo.countActiveBySystem();
                bySystem = systemStats.stream()
                        .map(row -> new DashboardSummaryResponse.BySystem(
                                row[0] != null ? String.valueOf(row[0]) : "UNCLASSIFIED",
                                row[1] != null ? String.valueOf(row[1]) : "Não classificado",
                                row[2] != null ? ((Number) row[2]).longValue() : 0L
                        ))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("⚠️ Erro ao buscar stats por sistema: {}", e.getMessage());
                bySystem = List.of();
            }

            // 6. Por status (baseado em issues abertas)
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

            log.info("✅ Dashboard: total={} OK={} issues={} duplicates={}",
                    totalArticles, okCount, issuesCount, duplicatesCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao gerar dashboard summary: {}", e.getMessage(), e);
            // Retorna resposta vazia em vez de 500
            return ResponseEntity.ok(new DashboardSummaryResponse(
                    0L, 0L, 0L, 0L, List.of(), List.of()
            ));
        }
    }
}
