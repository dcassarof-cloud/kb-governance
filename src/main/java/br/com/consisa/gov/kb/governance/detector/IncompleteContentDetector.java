package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.governance.KbContentAnalysisService;
import br.com.consisa.gov.kb.service.KbGovernanceIssueService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.stereotype.Component;

/**
 * Detector: conteúdo incompleto.
 */
@Component
public class IncompleteContentDetector {

    private static final int MIN_CHARS = 500;

    private final KbContentAnalysisService analysis;
    private final KbGovernanceIssueService issueService;

    public IncompleteContentDetector(KbContentAnalysisService analysis,
                                     KbGovernanceIssueService issueService) {
        this.analysis = analysis;
        this.issueService = issueService;
    }

    public void analyze(KbArticle article) {
        if (article == null || article.getId() == null) return;

        String text = article.getContentText();
        String html = article.getContentHtml();

        int textLen = analysis.length(text);
        int htmlLen = analysis.length(html);

        boolean emptyBoth = (textLen == 0 && htmlLen == 0);

        String base = (textLen > 0 ? text : html);
        String normalized = analysis.normalize(base);

        boolean tooShort = (!emptyBoth) && ((textLen > 0 ? textLen : htmlLen) < MIN_CHARS);
        boolean placeholder = analysis.hasPlaceholder(normalized);

        // ✅ Decide severidade corretamente (evita poluir com WARN em textos grandes)
        GovernanceSeverity severity;
        if (emptyBoth) {
            severity = GovernanceSeverity.ERROR;
        } else if (tooShort && placeholder) {
            severity = GovernanceSeverity.WARN;
        } else if (tooShort) {
            severity = GovernanceSeverity.WARN;
        } else if (placeholder) {
            severity = GovernanceSeverity.INFO; // texto grande mas com marcador
        } else {
            return; // conteúdo ok, não abre issue
        }

        String msg = buildMessage(emptyBoth, tooShort, placeholder, textLen, htmlLen);

        ObjectNode evidence = JsonNodeFactory.instance.objectNode();
        evidence.put("textLen", textLen);
        evidence.put("htmlLen", htmlLen);
        evidence.put("minChars", MIN_CHARS);
        evidence.put("placeholder", placeholder);
        evidence.put("emptyBoth", emptyBoth);

        issueService.open(
                article.getId(),
                KbGovernanceIssueType.INCOMPLETE_CONTENT,
                severity,
                msg,
                evidence
        );
    }


    private static String buildMessage(boolean emptyBoth,
                                       boolean tooShort,
                                       boolean placeholder,
                                       int textLen,
                                       int htmlLen) {

        if (emptyBoth) return "Conteúdo vazio (HTML e TEXT) — incompleto.";

        StringBuilder sb = new StringBuilder("Possível conteúdo incompleto: ");
        boolean first = true;

        if (tooShort) {
            sb.append("muito curto");
            first = false;
        }
        if (placeholder) {
            if (!first) sb.append(" + ");
            sb.append("placeholder detectado");
        }

        sb.append(String.format(" (textLen=%d, htmlLen=%d)", textLen, htmlLen));
        return sb.toString();
    }
}
