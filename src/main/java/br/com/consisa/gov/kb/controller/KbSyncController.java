package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.controller.dto.SyncConfigUpdateRequest;
import br.com.consisa.gov.kb.domain.KbSyncConfig;
import br.com.consisa.gov.kb.domain.KbSyncRun;
import br.com.consisa.gov.kb.domain.SyncMode;
import br.com.consisa.gov.kb.service.KbSyncOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kb/sync")
@PreAuthorize("hasRole('ADMIN')")
public class KbSyncController {

    private final KbSyncOrchestratorService svc;

    public KbSyncController(KbSyncOrchestratorService svc) {
        this.svc = svc;
    }

    @PostMapping("/run")
    public ResponseEntity<KbSyncRun> run(
            @RequestParam(defaultValue = "DELTA") String mode,
            @RequestParam(required = false) Integer daysBack
    ) {
        SyncMode resolvedMode = SyncMode.fromJson(mode);
        return ResponseEntity.ok(svc.runNow(resolvedMode, daysBack));
    }

    @GetMapping("/config")
    public ResponseEntity<KbSyncConfig> getConfig() {
        return ResponseEntity.ok(svc.getConfig());
    }

    @PutMapping( "/config")
    public ResponseEntity<KbSyncConfig> update(@RequestBody SyncConfigUpdateRequest req) {
        KbSyncConfig cfg = new KbSyncConfig();
        cfg.setEnabled(req.enabled);
        cfg.setMode(req.mode);
        cfg.setIntervalMinutes(req.intervalMinutes);
        cfg.setDaysBack(req.daysBack);
        return ResponseEntity.ok(svc.updateConfig(cfg));
    }

    @GetMapping("/runs/latest")
    public ResponseEntity<KbSyncRun> latest() {
        return ResponseEntity.ok(svc.latestRun());
    }
}
