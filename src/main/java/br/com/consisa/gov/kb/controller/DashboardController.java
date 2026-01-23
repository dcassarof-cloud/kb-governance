package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.dto.DashboardSummaryDto;
import br.com.consisa.gov.kb.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard API
 * GET /api/v1/dashboard/summary
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> summary() {
        return ResponseEntity.ok(service.getSummary());
    }
}
