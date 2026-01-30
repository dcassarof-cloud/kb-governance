package br.com.consisa.gov.kb.governance;

import br.com.consisa.gov.kb.domain.KbArticle;

/**
 * Interface comum para detectores de governança.
 */
public interface KbGovernanceDetector {
    void analyze(KbArticle article);
}
