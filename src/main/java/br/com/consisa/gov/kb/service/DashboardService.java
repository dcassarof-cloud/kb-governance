package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.dto.DashboardSummaryDto;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private final KbArticleRepository articleRepo;
    private final KbGovernanceIssueRepository issueRepo;

    public DashboardService(KbArticleRepository articleRepo, KbGovernanceIssueRepository issueRepo) {
        this.articleRepo = articleRepo;
        this.issueRepo = issueRepo;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        long totalArticles = articleRepo.countActiveArticles();

        // “OK” no teu schema é APPROVED (status do fluxo)
        long okCount = articleRepo.countActiveByGovernanceStatus("APPROVED");

        long issuesCount = issueRepo.countByStatus(GovernanceIssueStatus.OPEN);

        // Duplicados = issues abertas do tipo DUPLICATE_CONTENT (alinha com teu detector)
        long duplicatesCount = issueRepo.countByStatusAndIssueType(
                GovernanceIssueStatus.OPEN,
                KbGovernanceIssueType.DUPLICATE_CONTENT
        );

        List<DashboardSummaryDto.BySystem> bySystem = articleRepo.countActiveBySystem().stream()
                .map(r -> new DashboardSummaryDto.BySystem(
                        String.valueOf(r[0]),
                        String.valueOf(r[1]),
                        ((Number) r[2]).longValue()
                ))
                .toList();

        List<DashboardSummaryDto.ByStatus> byStatus = articleRepo.countActiveByGovernanceStatus().stream()
                .map(r -> new DashboardSummaryDto.ByStatus(
                        String.valueOf(r[0]),
                        ((Number) r[1]).longValue()
                ))
                .toList();

        return new DashboardSummaryDto(
                totalArticles,
                okCount,
                issuesCount,
                duplicatesCount,
                bySystem,
                byStatus
        );
    }
}
