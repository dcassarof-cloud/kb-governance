package br.com.consisa.gov.kb.governance;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.governance.detector.DuplicateContentDetector;
import br.com.consisa.gov.kb.governance.detector.IncompleteContentDetector;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestrador de detectores de governança.
 *
 * Ele serve para:
 * - rodar detectores em um artigo (analyzeArticle)
 * - rodar em lote (recentes)
 * - rodar duplicados (todos ou por hash)
 */
@Service
public class KbGovernanceDetectorService {

    private final KbArticleRepository articleRepo;
    private final IncompleteContentDetector incomplete;
    private final DuplicateContentDetector duplicate;

    public KbGovernanceDetectorService(
            KbArticleRepository articleRepo,
            IncompleteContentDetector incomplete,
            DuplicateContentDetector duplicate
    ) {
        this.articleRepo = articleRepo;
        this.incomplete = incomplete;
        this.duplicate = duplicate;
    }

    /**
     * Analisa um artigo chamando todos os detectores "por artigo".
     * (Duplicados normalmente é por hash / lote, mas pode rodar aqui também se quiser no futuro.)
     */
    @Transactional
    public void analyzeArticle(KbArticle article) {
        if (article == null) return;

        // 1) incompleto (placeholder, vazio, etc.)
        incomplete.analyze(article);

        // (2) outros detectores por artigo entram aqui depois
        // inconsistent.analyze(article);
        // outdated.analyze(article);
    }

    /**
     * Analisa os artigos mais recentes (por updated/created).
     *
     * @return quantos artigos foram processados
     */
    @Transactional
    public int analyzeRecent(int limit) {
        if (limit <= 0) limit = 50;

        var page = articleRepo.findRecent(PageRequest.of(0, limit));
        page.forEach(this::analyzeArticle);

        return page.getNumberOfElements();
    }

    // ============================
    // DUPLICADOS
    // ============================

    @Transactional
    public int analyzeDuplicates() {
        return duplicate.analyzeAllDuplicates();
    }

    @Transactional
    public int analyzeAllDuplicates() {
        return duplicate.analyzeAllDuplicates();
    }

    @Transactional
    public int analyzeHash(String hash) {
        return duplicate.analyzeHash(hash);
    }
}
