package br.com.consisa.gov.kb.controller.api;

import br.com.consisa.gov.kb.controller.api.dto.SyncConfigResponse;
import br.com.consisa.gov.kb.controller.api.dto.SyncConfigUpdateRequest;
import br.com.consisa.gov.kb.controller.api.dto.SyncRunLatestResponse;
import br.com.consisa.gov.kb.controller.api.dto.SyncRunResponse;
import br.com.consisa.gov.kb.controller.api.dto.SyncRunStartResponse;
import br.com.consisa.gov.kb.controller.api.dto.TriggerSyncRequest;
import br.com.consisa.gov.kb.domain.KbSyncConfig;
import br.com.consisa.gov.kb.domain.KbSyncRun;
import br.com.consisa.gov.kb.domain.SyncMode;
import br.com.consisa.gov.kb.domain.SyncRunStatus;
import br.com.consisa.gov.kb.repository.KbSyncRunRepository;
import br.com.consisa.gov.kb.service.GovernanceLanguageService;
import br.com.consisa.gov.kb.service.KbSyncOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
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
    private final GovernanceLanguageService languageService;

    public SyncApiController(
            KbSyncOrchestratorService orchestratorService,
            KbSyncRunRepository syncRunRepo,
            GovernanceLanguageService languageService
    ) {
        this.orchestratorService = orchestratorService;
        this.syncRunRepo = syncRunRepo;
        this.languageService = languageService;
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
     * POST /api/v1/sync/run?mode={FULL|INCREMENTAL|DELTA|DELTA_WINDOW}&daysBack={int}
     */
    @PostMapping("/run")
    public ResponseEntity<SyncRunStartResponse> runSync(
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer daysBack,
            @RequestBody(required = false) TriggerSyncRequest request
    ) {
        SyncMode resolvedMode = parseMode(mode != null ? mode : (request != null ? request.mode() : null));
        Integer safeDaysBack = normalizeDaysBack(daysBack != null ? daysBack : (request != null ? request.daysBack() : null));

        log.info("POST /api/v1/sync/run mode={} daysBack={}", resolvedMode, safeDaysBack);

        try {
            KbSyncRun run = orchestratorService.runNow(resolvedMode, safeDaysBack);
            return ResponseEntity.ok(new SyncRunStartResponse(
                    run.getId() != null ? run.getId().toString() : null,
                    normalizeMode(run.getMode()),
                    run.getStartedAt(),
                    normalizeStatus(run.getStatus()),
                    "Sync iniciado"
            ));
        } catch (IllegalStateException ex) {
            log.warn("⚠️ {}", ex.getMessage());
            return ResponseEntity.status(409).body(new SyncRunStartResponse(
                    null,
                    normalizeMode(resolvedMode),
                    null,
                    "FAILED",
                    ex.getMessage()
            ));
        }
    }

    /**
     * GET /api/v1/sync/runs/latest
     */
    @GetMapping("/runs/latest")
    public ResponseEntity<SyncRunLatestResponse> getLatestRun() {
        log.info("GET /api/v1/sync/runs/latest");

        return syncRunRepo.findTop1ByOrderByStartedAtDesc()
                .map(run -> ResponseEntity.ok(new SyncRunLatestResponse(
                        run.getId() != null ? run.getId().toString() : null,
                        normalizeMode(run.getMode()),
                        run.getStartedAt(),
                        run.getFinishedAt(),
                        normalizeStatus(run.getStatus()),
                        mapStats(run)
                )))
                .orElseGet(() -> ResponseEntity.ok(new SyncRunLatestResponse(
                        null,
                        null,
                        null,
                        null,
                        null,
                        new SyncRunLatestResponse.Stats(0, 0, 0, 0)
                )));
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
                languageService.syncRunStatusLabel(run.getStatus()),
                languageService.syncModeLabel(run.getMode()),
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
                languageService.syncModeLabel(config.getMode()),
                config.getIntervalMinutes(),
                config.getDaysBack()
        );
    }

    private SyncMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return SyncMode.DELTA_WINDOW;
        }
        String normalized = mode.trim().toUpperCase();
        return switch (normalized) {
            case "FULL" -> SyncMode.FULL;
            case "INCREMENTAL", "DELTA", "DELTA_WINDOW" -> SyncMode.DELTA_WINDOW;
            default -> throw new org.springframework.web.server.ResponseStatusException(
                    BAD_REQUEST, "Modo inválido: " + mode
            );
        };
    }

    private Integer normalizeDaysBack(Integer daysBack) {
        if (daysBack == null) {
            return null;
        }
        if (daysBack < 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    BAD_REQUEST, "daysBack deve ser maior ou igual a 0"
            );
        }
        return daysBack;
    }

    private String normalizeMode(SyncMode mode) {
        if (mode == null) {
            return null;
        }
        return mode == SyncMode.DELTA_WINDOW ? "DELTA" : mode.name();
    }

    private String normalizeStatus(SyncRunStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case RUNNING -> "RUNNING";
            case SUCCESS -> "COMPLETED";
            case FAILED -> "FAILED";
        };
    }

    private SyncRunLatestResponse.Stats mapStats(KbSyncRun run) {
        int processed = run.getSyncedCount() + run.getUpdatedCount()
                + run.getSkippedCount() + run.getNotFoundCount();
        return new SyncRunLatestResponse.Stats(
                processed,
                run.getSyncedCount(),
                run.getUpdatedCount(),
                run.getErrorCount()
        );
    }
}
