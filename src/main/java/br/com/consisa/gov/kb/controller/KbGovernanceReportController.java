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
 * Endpoints:
 * - Relatório completo
 * - Filtrado por sistema
 * - Apenas com problemas
 * - Apenas IA-Ready
 * - Estatísticas agregadas
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
     *
     * Retorna análise de TODOS os artigos com:
     * - Flags de problemas
     * - Lista de ações necessárias
     * - Score de qualidade
     * - Flag IA-Ready
     *
     * Exemplo de resposta:
     * [
     *   {
     *     "articleId": 123,
     *     "systemCode": "NOTAON",
     *     "title": "Cancelar NFS-e",
     *     "actions": [
     *       "MANUAL_SEM_ESTRUTURA_MINIMA",
     *       "MANUAL_CURTO_DEMAIS"
     *     ],
     *     "qualityScore": 40,
     *     "iaReady": false
     *   }
     * ]
     */
    @GetMapping
    public ResponseEntity<List<KbArticleGovernanceReportDto>> getFullReport() {
        return ResponseEntity.ok(service.generateFullReport());
    }

    /**
     * 🔍 Relatório filtrado por sistema
     *
     * GET /kb/governance/report?systemCode=NOTAON
     *
     * Retorna apenas artigos do sistema especificado
     */
    @GetMapping(params = "systemCode")
    public ResponseEntity<List<KbArticleGovernanceReportDto>> getReportBySystem(
            @RequestParam String systemCode
    ) {
        return ResponseEntity.ok(service.generateReportBySystem(systemCode));
    }

    /**
     * ⚠️ Relatório apenas de artigos COM PROBLEMAS
     *
     * GET /kb/governance/report/issues
     *
     * Retorna apenas artigos que precisam de ação
     * (vazio, curto, duplicado ou sem estrutura)
     */
    @GetMapping("/issues")
    public ResponseEntity<List<KbArticleGovernanceReportDto>> getIssuesOnly() {
        return ResponseEntity.ok(service.generateIssuesOnlyReport());
    }

    /**
     * ✅ Relatório de artigos IA-READY
     *
     * GET /kb/governance/report/ia-ready
     *
     * Retorna apenas artigos que atendem critérios mínimos para IA:
     * - Não vazio
     * - Não duplicado no mesmo sistema
     * - Tem estrutura mínima
     */
    @GetMapping("/ia-ready")
    public ResponseEntity<List<KbArticleGovernanceReportDto>> getIaReady() {
        return ResponseEntity.ok(service.generateIaReadyReport());
    }

    /**
     * 📈 Estatísticas agregadas
     *
     * GET /kb/governance/report/stats
     *
     * Retorna resumo geral:
     * {
     *   "total": 1203,
     *   "emptyCount": 45,
     *   "shortCount": 123,
     *   "duplicateCount": 28,
     *   "hashReusedCount": 12,
     *   "noStructureCount": 347,
     *   "iaReadyCount": 650,
     *   "iaReadyPercentage": 54.03
     * }
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(service.getSummaryStatistics());
    }
}