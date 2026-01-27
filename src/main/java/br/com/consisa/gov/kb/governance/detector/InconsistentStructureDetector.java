package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.governance.KbContentAnalysisService;
import br.com.consisa.gov.kb.service.KbGovernanceIssueService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Detector: INCONSISTENT_CONTENT
 *
 * Uso atual:
 * - Representa "SEM ESTRUTURA MÍNIMA"
 * - Modo forçado para reforma geral da base
 */
@Component
public class InconsistentStructureDetector {

    private final KbContentAnalysisService analysis;
    private final KbGovernanceIssueService issueService;

    public InconsistentStructureDetector(KbContentAnalysisService analysis,
                                         KbGovernanceIssueService issueService) {
        this.analysis = analysis;
        this.issueService = issueService;
    }

    public void analyze(KbArticle article) {
        if (article == null || article.getId() == null) return;

        int textLen = analysis.length(article.getContentText());
        int htmlLen = analysis.length(article.getContentHtml());

        GovernanceSeverity severity = GovernanceSeverity.WARN;

        String msg = "Manual sem estrutura mínima (revisão geral forçada).";

        ObjectNode evidence = JsonNodeFactory.instance.objectNode();
        evidence.put("forced", true);
        evidence.put("textLen", textLen);
        evidence.put("htmlLen", htmlLen);
        evidence.put("reason", "NO_MIN_STRUCTURE");

        issueService.open(
                article.getId(),
                KbGovernanceIssueType.INCONSISTENT_CONTENT,
                severity,
                msg,
                evidence
        );
    }
}
