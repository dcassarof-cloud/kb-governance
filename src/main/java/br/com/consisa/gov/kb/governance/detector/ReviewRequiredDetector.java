package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.governance.KbGovernanceDetector;
import br.com.consisa.gov.kb.service.KbGovernanceIssueService;
import org.springframework.stereotype.Component;

@Component
public class ReviewRequiredDetector implements KbGovernanceDetector {

    private final KbGovernanceIssueService issueService;

    public ReviewRequiredDetector(KbGovernanceIssueService issueService) {
        this.issueService = issueService;
    }

    @Override
    public void analyze(KbArticle article) {
        if (article == null || article.getId() == null) {
            return;
        }
        issueService.open(
                article.getId(),
                KbGovernanceIssueType.REVIEW_REQUIRED,
                GovernanceSeverity.INFO,
                "Revisão obrigatória pendente para este manual.",
                null
        );
    }
}
