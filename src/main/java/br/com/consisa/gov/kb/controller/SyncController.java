package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.dto.SyncRunDto;
import br.com.consisa.gov.kb.dto.SyncTriggerResponseDto;
import br.com.consisa.gov.kb.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Sync API
 * GET  /api/v1/sync/runs
 * POST /api/v1/sync/runs
 */
@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService service;

    public SyncController(SyncService service) {
        this.service = service;
    }

    @GetMapping("/runs")
    public ResponseEntity<List<SyncRunDto>> runs() {
        return ResponseEntity.ok(service.listRuns());
    }

    @PostMapping("/runs")
    public ResponseEntity<SyncTriggerResponseDto> trigger() {
        return ResponseEntity.ok(service.triggerSync());
    }
}
