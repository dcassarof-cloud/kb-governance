package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.service.KbGovernanceReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📊 Controller Avançado de Governança
 *
 * ENDPOINTS DISPONÍVEIS:
 * ----------------------
 * GET  /kb/governance/advanced/dashboard       - Dashboard consolidado
 * GET  /kb/governance/advanced/metrics/summary - Métricas agregadas
 * GET  /kb/governance/advanced/articles/issues - Artigos com problemas
 * GET  /kb/governance/advanced/articles/ready  - Artigos IA-Ready
 *
 * FUNCIONALIDADES:
 * ----------------
 * - Dashboard consolidado com múltiplas métricas
 * - Visão agregada de qualidade
 * - Listagem de artigos problemáticos
 * - Análise de artigos prontos para IA
 *
 * INTEGRAÇÃO:
 * -----------
 * - KbGovernanceReportService: Estatísticas e relatórios
 * - KbArticleRepository: Acesso direto aos artigos
 */
@RestController
@RequestMapping("/kb/governance/advanced")
@CrossOrigin(origins = "*")
public class GovernanceAdvancedController {

    private static final Logger log = LoggerFactory.getLogger(GovernanceAdvancedController.class);

    private final KbGovernanceReportService reportService;
    private final KbArticleRepository articleRepository;
    private final KbGovernanceIssueRepository issueRepository;

    public GovernanceAdvancedController(
            KbGovernanceReportService reportService,
            KbArticleRepository articleRepository,
            KbGovernanceIssueRepository issueRepository
    ) {
        this.reportService = reportService;
        this.articleRepository = articleRepository;
        this.issueRepository = issueRepository;
    }

    // ==================== DASHBOARD CONSOLIDADO ====================

    /**
     * 📊 Dashboard consolidado com todas as métricas
     *
     * GET /kb/governance/advanced/dashboard
     *
     * Retorna:
     * - Estatísticas gerais
     * - Distribuição por sistema
     * - Top problemas
     * - Progresso IA-Ready
     *
     * Exemplo de resposta:
     * {
     *   "summary": {
     *     "total": 1103,
     *     "iaReady": 856,
     *     "withIssues": 247,
     *     "iaReadyPercentage": 77.6
     *   },
     *   "byIssueType": {
     *     "empty": 12,
     *     "short": 45,
     *     "duplicate": 23,
     *     "noStructure": 167
     *   },
     *   "topSystems": [...]
     * }
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        log.info("GET /kb/governance/advanced/dashboard");

        try {
            // 1) Estatísticas gerais
            Map<String, Object> stats = reportService.getSummaryStatistics();

            // 2) Monta dashboard
            Map<String, Object> dashboard = new HashMap<>();

            // Summary
            Map<String, Object> summary = new HashMap<>();
            long totalArticles = articleRepository.count();
            long articlesWithIssues = issueRepository.countDistinctArticlesWithIssues();
            long articlesOk = Math.max(0, totalArticles - articlesWithIssues);
            summary.put("total", totalArticles);
            summary.put("iaReady", stats.get("iaReadyCount"));
            summary.put("withIssues", articlesWithIssues);
            summary.put("ok", articlesOk);
            summary.put("iaReadyPercentage", stats.get("iaReadyPercentage"));
            dashboard.put("summary", summary);

            // Por tipo de problema
            Map<String, Object> byIssueType = new HashMap<>();
            byIssueType.put("empty", stats.get("emptyCount"));
            byIssueType.put("short", stats.get("shortCount"));
            byIssueType.put("duplicate", stats.get("duplicateCount"));
            byIssueType.put("noStructure", stats.get("noStructureCount"));
            byIssueType.put("hashReused", stats.get("hashReusedCount"));
            dashboard.put("byIssueType", byIssueType);

            // Contadores por sistema
            List<Object[]> systemStats = articleRepository.countBySystemAndSyncStatus();
            dashboard.put("bySystems", systemStats);

            // Timestamp
            dashboard.put("generatedAt", java.time.OffsetDateTime.now());

            log.info("✅ Dashboard gerado: {} total, {} IA-Ready ({}%)",
                    stats.get("total"),
                    stats.get("iaReadyCount"),
                    stats.get("iaReadyPercentage"));

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("❌ Erro ao gerar dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== MÉTRICAS AGREGADAS ====================

    /**
     * 📈 Métricas agregadas de governança
     *
     * GET /kb/governance/advanced/metrics/summary
     *
     * Retorna estatísticas consolidadas sem detalhes de artigos.
     * Útil para widgets e indicadores.
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        log.info("GET /kb/governance/advanced/metrics/summary");

        try {
            Map<String, Object> stats = reportService.getSummaryStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar métricas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== ARTIGOS COM PROBLEMAS ====================

    /**
     * ⚠️ Lista artigos com problemas de qualidade
     *
     * GET /kb/governance/advanced/articles/issues?limit=50
     *
     * Retorna artigos que precisam de atenção.
     * Ordenados por gravidade (vazios > duplicados > sem estrutura).
     *
     * @param limit máximo de artigos (default: 50, max: 500)
     */
    @GetMapping("/articles/issues")
    public ResponseEntity<Map<String, Object>> getArticlesWithIssues(
            @RequestParam(defaultValue = "50") int limit
    ) {
        log.info("GET /kb/governance/advanced/articles/issues?limit={}", limit);

        try {
            int safeLimit = Math.min(Math.max(limit, 1), 500);

            var report = reportService.generateIssuesOnlyReport();

            // Limita resultado
            var limitedReport = report.stream()
                    .limit(safeLimit)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("total", report.size());
            response.put("showing", limitedReport.size());
            response.put("articles", limitedReport);

            log.info("✅ Retornando {} artigos com problemas (de {} totais)",
                    limitedReport.size(), report.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar artigos com problemas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== ARTIGOS IA-READY ====================

    /**
     * ✅ Lista artigos prontos para IA
     *
     * GET /kb/governance/advanced/articles/ready?limit=100
     *
     * Retorna artigos sem problemas críticos.
     * Prontos para uso em sistemas de IA/RAG.
     *
     * @param limit máximo de artigos (default: 100, max: 1000)
     */
    @GetMapping("/articles/ready")
    public ResponseEntity<Map<String, Object>> getIaReadyArticles(
            @RequestParam(defaultValue = "100") int limit
    ) {
        log.info("GET /kb/governance/advanced/articles/ready?limit={}", limit);

        try {
            int safeLimit = Math.min(Math.max(limit, 1), 1000);

            var report = reportService.generateIaReadyReport();

            // Limita resultado
            var limitedReport = report.stream()
                    .limit(safeLimit)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("total", report.size());
            response.put("showing", limitedReport.size());
            response.put("articles", limitedReport);

            log.info("✅ Retornando {} artigos IA-Ready (de {} totais)",
                    limitedReport.size(), report.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar artigos IA-Ready: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== MÉTRICAS POR SISTEMA ====================

    /**
     * 🗂️ Métricas de governança por sistema
     *
     * GET /kb/governance/advanced/metrics/by-system/{systemCode}
     *
     * Retorna estatísticas específicas de um sistema.
     *
     * @param systemCode código do sistema (ex: NOTAON, CONSISANET)
     */
    @GetMapping("/metrics/by-system/{systemCode}")
    public ResponseEntity<Map<String, Object>> getMetricsBySystem(
            @PathVariable String systemCode
    ) {
        log.info("GET /kb/governance/advanced/metrics/by-system/{}", systemCode);

        try {
            var report = reportService.generateReportBySystem(systemCode);

            long total = report.size();
            long iaReady = report.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getIaReady()))
                    .count();
            long withIssues = total - iaReady;

            Map<String, Object> response = new HashMap<>();
            response.put("systemCode", systemCode);
            response.put("total", total);
            response.put("iaReady", iaReady);
            response.put("withIssues", withIssues);
            response.put("iaReadyPercentage", total > 0 ? (iaReady * 100.0 / total) : 0.0);
            response.put("articles", report);

            log.info("✅ Sistema {}: {} total, {} IA-Ready",
                    systemCode, total, iaReady);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar métricas do sistema {}: {}",
                    systemCode, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== HEALTH CHECK ====================

    /**
     * 💚 Health check do módulo de governança
     *
     * GET /kb/governance/advanced/health
     *
     * Verifica se o sistema de governança está funcional.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Testa acesso ao report service
            reportService.getSummaryStatistics();

            health.put("status", "UP");
            health.put("service", "GovernanceAdvancedController");
            health.put("timestamp", java.time.OffsetDateTime.now());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("❌ Health check falhou: {}", e.getMessage(), e);

            health.put("status", "DOWN");
            health.put("error", e.getMessage());

            return ResponseEntity.status(503).body(health);
        }
    }
}
