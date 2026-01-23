package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.dto.DashboardSummaryDto;
import br.com.consisa.gov.kb.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard API - Resumo de governança
 *
 * Rotas:
 *  GET /api/v1/dashboard/summary
 *
 * Observação:
 * - O front já está chamando exatamente essa rota.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> summary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
