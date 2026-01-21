package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.scheduler.KbSyncScheduler;
import br.com.consisa.gov.kb.service.KbFullSyncService;
import br.com.consisa.gov.kb.service.KbSyncOrchestratorService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 📊 NOVO - Controller de Status e Métricas de Sync
 *
 * ENDPOINTS:
 * ----------
 * GET  /kb/sync/status        - Status geral do sync
 * GET  /kb/sync/progress      - Progresso em tempo real
 * GET  /kb/sync/metrics       - Métricas do scheduler
 * POST /kb/sync/retry-failed  - Retry de artigos que falharam
 *
 * USO:
 * ----
 * - Dashboard de monitoramento
 * - Integração com frontend
 * - Alertas e notificações
 * - Debugging de problemas
 */
@RestController
@RequestMapping("/kb/sync")
public class KbSyncStatusController {

    private final KbSyncOrchestratorService orchestrator;
    private final KbFullSyncService fullSync;
    private final KbSyncScheduler scheduler;

    public KbSyncStatusController(
            KbSyncOrchestratorService orchestrator,
            KbFullSyncService fullSync,
            KbSyncScheduler scheduler
    ) {
        this.orchestrator = orchestrator;
        this.fullSync = fullSync;
        this.scheduler = scheduler;
    }

    /**
     * 📊 Status geral do sistema de sync.
     *
     * GET /kb/sync/status
     *
     * Retorna:
     * - Estado atual (rodando/parado)
     * - Última execução
     * - Configuração atual
     * - Métricas do scheduler
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        var config = orchestrator.getConfig();
        var latestRun = orchestrator.latestRun();
        var schedulerMetrics = scheduler.getMetrics();

        return ResponseEntity.ok(Map.of(
                "isRunning", orchestrator.isRunning(),
                "config", config,
                "latestRun", latestRun != null ? latestRun : Map.of(),
                "scheduler", Map.of(
                        "lastSuccessfulRun", schedulerMetrics.lastSuccessfulRun,
                        "lastFailedRun", schedulerMetrics.lastFailedRun,
                        "consecutiveFailures", schedulerMetrics.consecutiveFailures,
                        "isWorkingHours", schedulerMetrics.isWorkingHours,
                        "isRunning", schedulerMetrics.isRunning
                )
        ));
    }

    /**
     * 📈 Progresso em tempo real.
     *
     * GET /kb/sync/progress
     *
     * Retorna métricas do sync em andamento:
     * - Artigos processados
     * - Sucessos/falhas
     * - Tempo médio por artigo
     * - Tempo decorrido
     *
     * Útil para:
     * - Barra de progresso no frontend
     * - Estimativa de conclusão
     * - Monitoramento de performance
     */
    @GetMapping("/progress")
    public ResponseEntity<?> getProgress() {
        if (!orchestrator.isRunning()) {
            return ResponseEntity.ok(Map.of(
                    "isRunning", false,
                    "message", "Nenhum sync em execução no momento"
            ));
        }

        var progress = fullSync.getProgress();

        return ResponseEntity.ok(Map.of(
                "isRunning", true,
                "processed", progress.processed,
                "succeeded", progress.succeeded,
                "failed", progress.failed,
                "elapsedMs", progress.elapsedMs,
                "avgTimePerArticleMs", progress.avgTimePerArticleMs
        ));
    }

    /**
     * 📊 Métricas do scheduler.
     *
     * GET /kb/sync/metrics
     *
     * Retorna informações sobre o scheduler:
     * - Última execução bem-sucedida
     * - Última falha
     * - Falhas consecutivas
     * - Estado atual
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        var metrics = scheduler.getMetrics();

        return ResponseEntity.ok(Map.of(
                "lastSuccessfulRun", metrics.lastSuccessfulRun,
                "lastFailedRun", metrics.lastFailedRun,
                "consecutiveFailures", metrics.consecutiveFailures,
                "isWorkingHours", metrics.isWorkingHours,
                "isRunning", metrics.isRunning,
                "syncInProgress", orchestrator.isRunning()
        ));
    }

    /**
     * 🔄 Retry de artigos que falharam.
     *
     * POST /kb/sync/retry-failed?limit=50
     *
     * Tenta sincronizar novamente artigos que falharam.
     *
     * Útil para:
     * - Recuperação após problemas temporários
     * - Reprocessamento manual
     * - Limpeza de erros
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<?> retryFailed(
            @RequestParam(defaultValue = "50") int limit
    ) {
        // TODO: Implementar no repository
        // List<Long> failedIds = articleRepo.findFailedArticles(PageRequest.of(0, limit));

        return ResponseEntity.ok(Map.of(
                "message", "Feature em desenvolvimento",
                "requestedLimit", limit
        ));
    }

    /**
     * 🛑 Para sync em execução (emergency stop).
     *
     * POST /kb/sync/stop
     *
     * ⚠️ ATENÇÃO: Só use em emergência!
     * O sync pode ficar em estado inconsistente.
     */
    @PostMapping("/stop")
    public ResponseEntity<?> emergencyStop() {
        // TODO: Implementar mecanismo de stop graceful
        return ResponseEntity.ok(Map.of(
                "message", "Feature em desenvolvimento",
                "warning", "Stop forçado pode causar inconsistências"
        ));
    }
}