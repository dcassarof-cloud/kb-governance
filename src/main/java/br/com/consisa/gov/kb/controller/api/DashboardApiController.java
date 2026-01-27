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
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        log.info("GET /api/v1/dashboard/summary");

        try {
            // 1. Total de artigos ativos
            long totalArticles = articleRepo.count();

            // 2. Issues abertas
            long issuesCount = issueRepo.count();

            // 3. Artigos OK (estimativa: total - artigos com issues)
            // Simplificação: artigos que não têm issues = OK
            long okCount = Math.max(0, totalArticles - issuesCount);

            // 4. Duplicados (hashes duplicados)
            List<String> duplicateHashes = articleRepo.findDuplicateContentHashes();
            long duplicatesCount = duplicateHashes.size();

            // 5. Por sistema (usando query nativa)
            List<Object[]> systemStats = articleRepo.countBySystemAndSyncStatus();
            List<DashboardSummaryResponse.BySystem> bySystem = systemStats.stream()
                    .map(row -> new DashboardSummaryResponse.BySystem(
                            (String) row[0],  // system_code
                            "",  // system_name (não temos na query)
                            ((Number) row[2]).longValue()  // count
                    ))
                    .collect(Collectors.toList());

            // 6. Por status (simplificado)
            List<DashboardSummaryResponse.ByStatus> byStatus = List.of(
                    new DashboardSummaryResponse.ByStatus("OK", okCount),
                    new DashboardSummaryResponse.ByStatus("WITH_ISSUES", issuesCount)
            );

            DashboardSummaryResponse response = new DashboardSummaryResponse(
                    totalArticles,
                    okCount,
                    issuesCount,
                    duplicatesCount,
                    bySystem,
                    byStatus
            );

            log.info("✅ Dashboard summary: {} total, {} OK, {} issues", 
                    totalArticles, okCount, issuesCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao gerar dashboard summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
