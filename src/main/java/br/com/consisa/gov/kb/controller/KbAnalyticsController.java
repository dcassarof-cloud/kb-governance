package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbArticleVersion;
import br.com.consisa.gov.kb.domain.KbGovernanceSnapshot;
import br.com.consisa.gov.kb.service.KbArticleVersionService;
import br.com.consisa.gov.kb.service.KbGovernanceSnapshotService;
import br.com.consisa.gov.kb.service.KbSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 📊 REST API - ANALYTICS, BUSCA E VERSIONAMENTO
 *
 * ENDPOINTS:
 * ----------
 * Analytics:
 * - GET /kb/analytics/trend/{days} - Tendência histórica
 * - GET /kb/analytics/system/{systemCode}/trend/{days} - Tendência por sistema
 * - GET /kb/analytics/compare/{days} - Comparação de períodos
 * - GET /kb/analytics/chart/{days} - Dados para gráficos
 *
 * Busca:
 * - GET /kb/search?q={query}&limit={limit} - Busca global
 * - GET /kb/search/system/{systemCode}?q={query} - Busca por sistema
 * - GET /kb/search/ia-ready?q={query} - Busca apenas IA-ready
 * - GET /kb/search/similar/{articleId} - Artigos relacionados
 *
 * Versionamento:
 * - GET /kb/articles/{id}/versions - Histórico de versões
 * - GET /kb/articles/{id}/versions/{versionNumber} - Versão específica
 * - GET /kb/articles/{id}/versions/latest - Última versão
 * - POST /kb/articles/{id}/versions - Criar nova versão
 * - POST /kb/articles/{id}/rollback/{versionNumber} - Rollback
 */
@RestController
@RequestMapping("/kb")
public class KbAnalyticsController {

    private final KbGovernanceSnapshotService snapshotService;
    private final KbSearchService searchService;
    private final KbArticleVersionService versionService;

    public KbAnalyticsController(
            KbGovernanceSnapshotService snapshotService,
            KbSearchService searchService,
            KbArticleVersionService versionService
    ) {
        this.snapshotService = snapshotService;
        this.searchService = searchService;
        this.versionService = versionService;
    }

    // ======================
    // ANALYTICS
    // ======================

    /**
     * GET /kb/analytics/trend/30
     * <p>
     * Retorna tendência dos últimos N dias (global)
     */
    @GetMapping("/analytics/trend/{days}")
    public ResponseEntity<List<KbGovernanceSnapshot>> getTrend(@PathVariable int days) {
        var trend = snapshotService.getTrend(days);
        return ResponseEntity.ok(trend);
    }

    /**
     * GET /kb/analytics/system/CONSISANET/trend/30
     * <p>
     * Retorna tendência de um sistema específico
     */
    @GetMapping("/analytics/system/{systemCode}/trend/{days}")
    public ResponseEntity<List<KbGovernanceSnapshot>> getSystemTrend(
            @PathVariable String systemCode,
            @PathVariable int days
    ) {
        var trend = snapshotService.getSystemTrend(systemCode, days);
        return ResponseEntity.ok(trend);
    }

    /**
     * GET /kb/analytics/compare/7
     * <p>
     * Compara período atual com período anterior (ex: última semana vs semana passada)
     */
    @GetMapping("/analytics/compare/{days}")
    public ResponseEntity<Map<String, Object>> comparePeriods(@PathVariable int days) {
        var comparison = snapshotService.comparePeriods(days);
        return ResponseEntity.ok(comparison);
    }

    /**
     * GET /kb/analytics/chart/30
     * <p>
     * Retorna dados formatados para gráficos (Chart.js, Recharts, etc)
     */
    @GetMapping("/analytics/chart/{days}")
    public ResponseEntity<Map<String, Object>> getChartData(@PathVariable int days) {
        var chartData = snapshotService.getChartData(days);
        return ResponseEntity.ok(chartData);
    }

    // ======================
    // BUSCA
    // ======================

    /**
     * GET /kb/search?q=como cadastrar cliente&limit=10
     * <p>
     * Busca global em todos os artigos
     */
    @GetMapping("/search")
    public ResponseEntity<List<KbSearchService.SearchResult>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        var results = searchService.search(q, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /kb/search/system/CONSISANET?q=nota fiscal
     * <p>
     * Busca apenas em artigos de um sistema específico
     */
    @GetMapping("/search/system/{systemCode}")
    public ResponseEntity<List<KbSearchService.SearchResult>> searchInSystem(
            @PathVariable String systemCode,
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        var results = searchService.searchInSystem(q, systemCode, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /kb/search/ia-ready?q=emitir nfe
     * <p>
     * Busca apenas em artigos IA-ready (alta qualidade)
     */
    @GetMapping("/search/ia-ready")
    public ResponseEntity<List<KbSearchService.SearchResult>> searchIaReady(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit
    ) {
        var results = searchService.searchIaReady(q, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /kb/search/similar/123
     * <p>
     * Retorna artigos relacionados/similares
     */
    @GetMapping("/search/similar/{articleId}")
    public ResponseEntity<List<KbSearchService.SearchResult>> findSimilar(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        var results = searchService.findSimilar(articleId, limit);
        return ResponseEntity.ok(results);
    }

    // ======================
    // VERSIONAMENTO
    // ======================

    /**
     * GET /kb/articles/123/versions
     * <p>
     * Retorna histórico completo de versões de um artigo
     */
    @GetMapping("/articles/{articleId}/versions")
    public ResponseEntity<List<KbArticleVersion>> getVersionHistory(@PathVariable Long articleId) {
        var history = versionService.getHistory(articleId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /kb/articles/123/versions/5
     * <p>
     * Retorna uma versão específica
     */
    @GetMapping("/articles/{articleId}/versions/{versionNumber}")
    public ResponseEntity<KbArticleVersion> getVersion(
            @PathVariable Long articleId,
            @PathVariable Integer versionNumber
    ) {
        var version = versionService.getVersion(articleId, versionNumber);
        return ResponseEntity.ok(version);
    }

    /**
     * GET /kb/articles/123/versions/latest
     * <p>
     * Retorna última versão
     */
    @GetMapping("/articles/{articleId}/versions/latest")
    public ResponseEntity<KbArticleVersion> getLatestVersion(@PathVariable Long articleId) {
        var version = versionService.getLatestVersion(articleId);
        return ResponseEntity.ok(version);
    }

    /**
     * POST /kb/articles/123/versions
     * Body: { "changedBy": "joao.silva", "reason": "Correção de erros", "changeType": "UPDATED" }
     * <p>
     * Cria nova versão manualmente (snapshot do estado atual)
     */
    @PostMapping("/articles/{articleId}/versions")
    public ResponseEntity<KbArticleVersion> createVersion(
            @PathVariable Long articleId,
            @RequestBody CreateVersionRequest request
    ) {
        var version = versionService.createVersion(
                articleId,
                request.changedBy(),
                request.reason(),
                request.changeType()
        );
        return ResponseEntity.ok(version);
    }

    /**
     * GET /kb/articles/123/versions/compare?a=2&b=5
     * <p>
     * Compara duas versões
     */
    @GetMapping("/articles/{articleId}/versions/compare")
    public ResponseEntity<KbArticleVersionService.VersionComparison> compareVersions(
            @PathVariable Long articleId,
            @RequestParam Integer a,
            @RequestParam Integer b
    ) {
        var comparison = versionService.compareVersions(articleId, a, b);
        return ResponseEntity.ok(comparison);
    }

    /**
     * POST /kb/articles/123/rollback/3
     * Body: { "rolledBackBy": "joao.silva" }
     * <p>
     * Restaura artigo para uma versão anterior
     */
    @PostMapping("/articles/{articleId}/rollback/{targetVersion}")
    public ResponseEntity<KbArticle> rollbackToVersion(
            @PathVariable Long articleId,
            @PathVariable Integer targetVersion,
            @RequestBody RollbackRequest request
    ) {
        var article = versionService.rollbackToVersion(articleId, targetVersion, request.rolledBackBy());
        return ResponseEntity.ok(article);
    }

    // ======================
    // DTOs
    // ======================

    public record CreateVersionRequest(
            String changedBy,
            String reason,
            String changeType
    ) {
    }

    public record RollbackRequest(
            String rolledBackBy
    ){}
}

