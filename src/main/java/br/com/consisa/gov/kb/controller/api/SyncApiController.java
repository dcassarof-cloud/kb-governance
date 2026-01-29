package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.SyncConfigResponse;
import br.com.consisa.gov.kb.controller.api.dto.SyncConfigUpdateRequest;
import br.com.consisa.gov.kb.controller.api.dto.SyncRunResponse;
import br.com.consisa.gov.kb.controller.api.dto.TriggerSyncRequest;
import br.com.consisa.gov.kb.domain.KbSyncConfig;
import br.com.consisa.gov.kb.domain.KbSyncRun;
import br.com.consisa.gov.kb.domain.SyncMode;
import br.com.consisa.gov.kb.repository.KbSyncRunRepository;
import br.com.consisa.gov.kb.service.KbSyncOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔄 Sync API Controller
 *
 * Endpoints:
 * - GET /api/v1/sync/runs (lista execuções)
 * - POST /api/v1/sync/runs (dispara sync)
 * - GET /api/v1/sync/config (busca config)
 * - PUT /api/v1/sync/config (atualiza config)
 */
@RestController
@RequestMapping("/api/v1/sync")
@CrossOrigin(origins = "*")
public class SyncApiController {

    private static final Logger log = LoggerFactory.getLogger(SyncApiController.class);

    private final KbSyncOrchestratorService orchestratorService;
    private final KbSyncRunRepository syncRunRepo;

    public SyncApiController(
            KbSyncOrchestratorService orchestratorService,
            KbSyncRunRepository syncRunRepo
    ) {
        this.orchestratorService = orchestratorService;
        this.syncRunRepo = syncRunRepo;
    }

    /**
     * GET /api/v1/sync/runs
     *
     * Retorna lista de execuções de sync (mais recentes primeiro).
     */
    @GetMapping("/runs")
    public ResponseEntity<List<SyncRunResponse>> getRuns() {
        log.info("GET /api/v1/sync/runs");

        try {
            List<KbSyncRun> runs = syncRunRepo.findAll(
                    Sort.by(Sort.Direction.DESC, "startedAt")
            );

            List<SyncRunResponse> response = runs.stream()
                    .limit(50)  // Limita a 50 mais recentes
                    .map(this::mapRunToDto)
                    .collect(Collectors.toList());

            log.info("✅ Retornando {} sync runs", response.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar sync runs: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/v1/sync/runs
     *
     * Dispara execução de sync.
     *
     * Body (opcional):
     * {
     *   "mode": "DELTA_WINDOW",
     *   "daysBack": 2,
     *   "note": "Sync manual"
     * }
     */
    @PostMapping("/runs")
    public ResponseEntity<SyncRunResponse> triggerSync(
            @RequestBody(required = false) TriggerSyncRequest request
    ) {
        log.info("POST /api/v1/sync/runs");

        try {
            SyncMode mode = SyncMode.DELTA_WINDOW;
            Integer daysBack = null;

            if (request != null) {
                if (request.mode() != null) {
                    // ✅ FIX: Converte INCREMENTAL para DELTA_WINDOW
                    String modeStr = request.mode().toUpperCase();
                    if ("INCREMENTAL".equals(modeStr)) {
                        mode = SyncMode.DELTA_WINDOW;
                        log.info("🔄 Convertendo mode INCREMENTAL → DELTA_WINDOW");
                    } else {
                        mode = SyncMode.valueOf(modeStr);
                    }
                }
                daysBack = request.daysBack();
            }

            log.info("🚀 Disparando sync: mode={} daysBack={}", mode, daysBack);

            KbSyncRun run = orchestratorService.runNow(mode, daysBack);

            SyncRunResponse response = mapRunToDto(run);

            log.info("✅ Sync disparado: id={} status={}", run.getId(), run.getStatus());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Sync já em execução
            log.warn("⚠️ {}", e.getMessage());
            return ResponseEntity.status(409).build();  // 409 Conflict

        } catch (Exception e) {
            log.error("❌ Erro ao disparar sync: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/v1/sync/run
     *
     * Alias para /runs (compatibilidade com botão do front).
     */
    @PostMapping("/run")
    public ResponseEntity<SyncRunResponse> triggerSyncAlias(
            @RequestBody(required = false) TriggerSyncRequest request
    ) {
        log.info("POST /api/v1/sync/run (alias)");
        return triggerSync(request);
    }

    /**
     * GET /api/v1/sync/config
     *
     * Retorna configuração de sync.
     */
    @GetMapping("/config")
    public ResponseEntity<SyncConfigResponse> getConfig() {
        log.info("GET /api/v1/sync/config");

        try {
            KbSyncConfig config = orchestratorService.getConfig();

            SyncConfigResponse response = mapConfigToDto(config);

            log.info("✅ Config: enabled={} mode={} interval={}",
                    config.isEnabled(), config.getMode(), config.getIntervalMinutes());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar config: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PUT /api/v1/sync/config
     *
     * Atualiza configuração de sync.
     *
     * Body:
     * {
     *   "enabled": true,
     *   "mode": "DELTA_WINDOW",
     *   "intervalMinutes": 60,
     *   "daysBack": 2
     * }
     */
    @PutMapping("/config")
    public ResponseEntity<SyncConfigResponse> updateConfig(
            @RequestBody SyncConfigUpdateRequest request
    ) {
        log.info("PUT /api/v1/sync/config");

        try {
            // ✅ FIX: Converte INCREMENTAL para DELTA_WINDOW
            String modeStr = request.mode().toUpperCase();
            if ("INCREMENTAL".equals(modeStr)) {
                modeStr = "DELTA_WINDOW";
                log.info("🔄 Convertendo mode INCREMENTAL → DELTA_WINDOW");
            }

            KbSyncConfig config = new KbSyncConfig();
            config.setEnabled(request.enabled());
            config.setMode(SyncMode.valueOf(modeStr));
            config.setIntervalMinutes(request.intervalMinutes());
            config.setDaysBack(request.daysBack());

            KbSyncConfig updated = orchestratorService.updateConfig(config);

            SyncConfigResponse response = mapConfigToDto(updated);

            log.info("✅ Config atualizada: enabled={} mode={} interval={}",
                    updated.isEnabled(), updated.getMode(), updated.getIntervalMinutes());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erro ao atualizar config: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ======================
    // MAPPING
    // ======================

    private SyncRunResponse mapRunToDto(KbSyncRun run) {
        return new SyncRunResponse(
                run.getId().toString(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getStatus().name(),
                run.getMode().name(),
                run.getNote(),
                new SyncRunResponse.Stats(
                        run.getSyncedCount() + run.getUpdatedCount(),  // articlesProcessed
                        run.getSyncedCount(),  // articlesCreated
                        run.getUpdatedCount(),  // articlesUpdated
                        run.getErrorCount()  // errors
                )
        );
    }

    private SyncConfigResponse mapConfigToDto(KbSyncConfig config) {
        return new SyncConfigResponse(
                config.isEnabled(),
                config.getMode().name(),
                config.getIntervalMinutes(),
                config.getDaysBack()
        );
    }
}
