package br.com.consisa.gov.kb.governance;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.governance.detector.DuplicateContentDetector;
import br.com.consisa.gov.kb.governance.detector.IncompleteContentDetector;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KbGovernanceDetectorService {

    private final KbArticleRepository articleRepo;
    private final IncompleteContentDetector incomplete;
    private final DuplicateContentDetector duplicate;

    public KbGovernanceDetectorService(KbArticleRepository articleRepo,
                                       IncompleteContentDetector incomplete,
                                       DuplicateContentDetector duplicate) {
        this.articleRepo = articleRepo;
        this.incomplete = incomplete;
        this.duplicate = duplicate;
    }

    /**
     * Analisa 1 artigo (útil para varreduras paginadas).
     * Aqui ficam "detectores por artigo" (conteúdo incompleto, inconsistências, etc).
     */
    @Transactional
    public void analyzeArticle(KbArticle article) {
        if (article == null) return;

        // Detector: conteúdo incompleto (ex: contentHtml/text vazio)
        incomplete.analyze(article);

        // futuros detectores:
        // inconsistent.analyze(article);
        // outdated.analyze(article);
    }

    /**
     * Analisa os mais recentes (retorna quantos analisou).
     * Obs: controller pode optar por chamar isso ou consultar repo direto.
     */
    @Transactional
    public int analyzeRecent(int limit) {
        int size = Math.max(1, limit);
        var page = articleRepo.findRecent(PageRequest.of(0, size));
        page.forEach(this::analyzeArticle);
        return page.getNumberOfElements();
    }

    /**
     * DUPLICADOS: roda para todos os hashes duplicados (retorna qtd issues).
     */
    @Transactional
    public int analyzeAllDuplicates() {
        return duplicate.analyzeAllDuplicates();
    }

    /**
     * DUPLICADOS: roda um hash específico (retorna qtd issues).
     */
    @Transactional
    public int analyzeHash(String hash) {
        return duplicate.analyzeHash(hash);
    }
}
