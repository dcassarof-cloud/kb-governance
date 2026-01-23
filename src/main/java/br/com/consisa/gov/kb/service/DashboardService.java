package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.dto.DashboardSummaryDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    public DashboardSummaryDto getSummary() {
        // TODO (fase 2): trocar por queries reais:
        // - total articles (kb_article)
        // - issues (kb_governance_issue)
        // - duplicates (group by content_hash)
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
