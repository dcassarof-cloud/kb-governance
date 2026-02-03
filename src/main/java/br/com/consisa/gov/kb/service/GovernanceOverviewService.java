package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.controller.api.dto.GovernanceOverviewResponse;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceOverviewResponse.OverviewTotals;
import br.com.consisa.gov.kb.controller.api.dto.GovernanceOverviewResponse.SystemOverview;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Serviço de overview gerencial para governança de KB.
 *
 * <p>Gera métricas agregadas de issues para dashboards gerenciais:
 * <ul>
 *   <li>Totais: issues abertas, críticas, não atribuídas, vencidas</li>
 *   <li>Por sistema: mesmas métricas + healthScore</li>
 * </ul>
 *
 * <p>HealthScore (0-100):
 * <pre>
 * score = 100 - (criticalOpen*5 + highOpen*3 + mediumOpen*1 + overdue*2 + unassigned*2)
 * </pre>
 * Onde:
 * <ul>
 *   <li>ERROR = critical (peso 5)</li>
 *   <li>WARN = high/medium (peso 3)</li>
 *   <li>INFO = low (peso 1)</li>
 * </ul>
 *
 * @author KB Governance Team
 * @since Sprint 5
 */
@Service
public class GovernanceOverviewService {

    private static final Logger log = LoggerFactory.getLogger(GovernanceOverviewService.class);

    private final KbGovernanceIssueRepository issueRepository;

    public GovernanceOverviewService(KbGovernanceIssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    /**
     * Gera o overview gerencial completo.
     *
     * @return overview com totais e métricas por sistema
     */
    @Transactional(readOnly = true)
    public GovernanceOverviewResponse generateOverview() {
        log.info("Gerando overview gerencial de governança");

        // Buscar totais
        OverviewTotals totals = fetchTotals();

        // Buscar por sistema
        List<SystemOverview> bySystem = fetchBySystem();

        OffsetDateTime generatedAt = OffsetDateTime.now(ZoneOffset.UTC);

        log.info("Overview gerado: open={}, critical={}, unassigned={}, overdue={}, systems={}",
                totals.open(), totals.criticalOpen(), totals.unassigned(), totals.overdue(), bySystem.size());

        return new GovernanceOverviewResponse(totals, bySystem, generatedAt);
    }

    /**
     * Busca totais agregados usando as views/queries.
     */
    private OverviewTotals fetchTotals() {
        var row = issueRepository.fetchOverviewTotals();
        return new OverviewTotals(
                row.getOpenCount(),
                row.getCriticalOpenCount(),
                row.getUnassignedCount(),
                row.getOverdueCount()
        );
    }

    /**
     * Busca métricas por sistema.
     */
    private List<SystemOverview> fetchBySystem() {
        return issueRepository.fetchOverviewBySystem().stream()
                .map(row -> new SystemOverview(
                        row.getSystemCode(),
                        row.getSystemName(),
                        row.getOpenCount(),
                        row.getCriticalCount(),
                        row.getOverdueCount(),
                        row.getUnassignedCount(),
                        calculateHealthScore(
                                row.getErrorCount(),
                                row.getWarnCount(),
                                row.getInfoCount(),
                                row.getOverdueCount(),
                                row.getUnassignedCount()
                        )
                ))
                .toList();
    }

    /**
     * Calcula o healthScore para um sistema.
     *
     * <p>Fórmula:
     * <pre>
     * score = 100 - (error*5 + warn*3 + info*1 + overdue*2 + unassigned*2)
     * </pre>
     *
     * <p>Resultado é limitado (clamped) entre 0 e 100.
     *
     * @return healthScore entre 0 e 100
     */
    public int calculateHealthScore(
            long errorCount,
            long warnCount,
            long infoCount,
            long overdueCount,
            long unassignedCount
    ) {
        long penalty = (errorCount * 5)
                + (warnCount * 3)
                + (infoCount * 1)
                + (overdueCount * 2)
                + (unassignedCount * 2);

        int score = (int) (100 - penalty);

        // Clamp entre 0 e 100
        return Math.max(0, Math.min(100, score));
    }
}
