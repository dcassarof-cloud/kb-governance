package br.com.consisa.gov.kb.governance;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.governance.detector.DuplicateContentDetector;
import br.com.consisa.gov.kb.governance.detector.IncompleteContentDetector;
import br.com.consisa.gov.kb.governance.detector.InconsistentStructureDetector;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço central de detecção de problemas de governança na KB.
 *
 * RESPONSABILIDADES:
 * - Rodar detectores "por artigo" (incompleto, sem estrutura, etc.)
 * - Rodar detectores "globais" (duplicados por hash, etc.)
 *
 * OBS IMPORTANTE (modo reforma geral):
 * - Neste momento, estamos marcando TODOS os manuais como "SEM_ESTRUTURA"
 *   reaproveitando o tipo INCONSISTENT_CONTENT (já existe no enum e não era usado).
 * - Isso é proposital para forçar revisão geral / reforma da base.
 */
@Service
public class KbGovernanceDetectorService {

    private final KbArticleRepository articleRepo;

    // Detectores por artigo
    private final IncompleteContentDetector incomplete;
    private final InconsistentStructureDetector inconsistentStructure;

    // Detectores globais
    private final DuplicateContentDetector duplicate;

    public KbGovernanceDetectorService(KbArticleRepository articleRepo,
                                       IncompleteContentDetector incomplete,
                                       InconsistentStructureDetector inconsistentStructure,
                                       DuplicateContentDetector duplicate) {
        this.articleRepo = articleRepo;
        this.incomplete = incomplete;
        this.inconsistentStructure = inconsistentStructure;
        this.duplicate = duplicate;
    }

    /**
     * Analisa 1 artigo (útil para varreduras paginadas).
     * Aqui ficam os detectores por artigo:
     * - conteúdo incompleto (vazio/curto/placeholder)
     * - sem estrutura (modo reforma: sempre abre INCONSISTENT_CONTENT)
     */
    @Transactional
    public void analyzeArticle(KbArticle article) {
        if (article == null || article.getId() == null) return;

        // 1) Conteúdo incompleto (vazio/curto/placeholder)
        incomplete.analyze(article);

        // 2) SEM_ESTRUTURA (reforma geral)
        // Reaproveita INCONSISTENT_CONTENT como "sem estrutura mínima"
        inconsistentStructure.analyze(article);

        // futuros detectores:
        // outdated.analyze(article);
        // inconsistent.analyze(article);
    }

    /**
     * Analisa os artigos mais recentes (retorna quantos analisou).
     * Controller pode chamar isso para rodar uma varredura rápida.
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
