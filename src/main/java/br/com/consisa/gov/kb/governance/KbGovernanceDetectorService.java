package br.com.consisa.gov.kb.governance;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.governance.detector.IncompleteContentDetector;
import org.springframework.stereotype.Service;

/**
 * Orquestra os detectores de governança.
 */
@Service
public class KbGovernanceDetectorService {

    private final IncompleteContentDetector incomplete;

    public KbGovernanceDetectorService(IncompleteContentDetector incomplete) {
        this.incomplete = incomplete;
    }

    public void analyzeArticle(KbArticle article) {
        incomplete.analyze(article);
    }
}
