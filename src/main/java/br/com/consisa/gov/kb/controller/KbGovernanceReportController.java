package br.com.consisa.gov.kb.controller;

import br.com.consisa.gov.kb.dto.KbArticleGovernanceReportDto;
import br.com.consisa.gov.kb.service.KbGovernanceReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller para relatório de governança dos manuais
 *
 * ✅ CORRIGIDO: Endpoints separados para evitar conflito de params
 */
@RestController
@RequestMapping("/kb/governance/report")
public class KbGovernanceReportController {

    private final KbGovernanceReportService service;

    public KbGovernanceReportController(KbGovernanceReportService service) {
        this.service = service;
    }

    /**
     * 📊 Relatório completo de todos os artigos
     *
     * GET /kb/governance/report
     */
    @GetMapping
    public ResponseEntity<List<KbArticleGovernanceReportDto>> getFullReport() {
        return ResponseEntity.ok(service.generateFullReport());
    }

    /**
     * 🔍 Relatório filtrado por sistema
     *
     * GET /kb/governance/report/by-system/{systemCode}
     *
     * Exemplo: GET /kb/governance/report/by-system/NOTAON
     */
    @GetMapping("/by-system/{systemCode}")
    public ResponseEntity<List<KbArticleGovernanceReportDto>> getReportBySystem(
            @PathVariable String systemCode
    ) {
        return ResponseEntity.ok(service.generateReportBySystem(systemCode));
    }

    /**
     * ⚠️ Relatório apenas de artigos COM PROBLEMAS
     *
     * GET /kb/governance/report/issues
     */
    @GetMapping("/issues")
    public ResponseEntity<List<KbArticleGovernanceReportDto>> getIssuesOnly() {
        return ResponseEntity.ok(service.generateIssuesOnlyReport());
    }

    /**
     * ✅ Relatório de artigos IA-READY
     *
     * GET /kb/governance/report/ia-ready
     */
    @GetMapping("/ia-ready")
    public ResponseEntity<List<KbArticleGovernanceReportDto>> getIaReady() {
        return ResponseEntity.ok(service.generateIaReadyReport());
    }

    /**
     * 📈 Estatísticas agregadas
     *
     * GET /kb/governance/report/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(service.getSummaryStatistics());
    }
}