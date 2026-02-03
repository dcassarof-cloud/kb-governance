package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.governance.KbGovernanceDetector;
import org.springframework.stereotype.Component;

@Component
public class AiReadyDetector implements KbGovernanceDetector {

    private final AiReadyAuditService auditService;

    public AiReadyDetector(AiReadyAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void analyze(KbArticle article) {
        auditService.audit(article);
    }
}
