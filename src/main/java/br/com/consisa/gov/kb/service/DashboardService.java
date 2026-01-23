package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.dto.DashboardSummaryDto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço do Dashboard.
 *
 * Comece simples (stub) e depois conecte ao banco:
 * - contar artigos
 * - contar issues
 * - contar duplicados
 * - agrupamentos por sistema/status
 */
@Service
public class DashboardService {

    public DashboardSummaryDto getSummary() {
        // TODO: Buscar dados reais do Postgres (repositories)
        return new DashboardSummaryDto(
                0,
                0,
                0,
                0,
                List.of(),
                List.of()
        );
    }
}
