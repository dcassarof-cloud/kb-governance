package br.com.consisa.gov.kb.governance;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.governance.detector.DuplicateContentDetector;
import br.com.consisa.gov.kb.governance.detector.IncompleteContentDetector;
import br.com.consisa.gov.kb.governance.detector.InconsistentStructureDetector;
import br.com.consisa.gov.kb.governance.detector.OutdatedContentDetector;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🔍 Serviço central de detecção de problemas de governança na KB.
 *
 * DETECTORES POR ARTIGO:
 * - INCOMPLETE_CONTENT: conteúdo vazio/curto/placeholder
 * - INCONSISTENT_CONTENT: sem sistema ou em sistema genérico
 * - OUTDATED_CONTENT: não atualizado há mais de X dias
 *
 * DETECTORES GLOBAIS:
 * - DUPLICATE_CONTENT: artigos com mesmo hash de conteúdo
 *
 * IDEMPOTÊNCIA:
 * - Cada detector usa KbGovernanceIssueService.open() que é idempotente
 * - Não cria issues duplicadas para o mesmo artigo/tipo
 */
@Service
public class KbGovernanceDetectorService {

    private static final Logger log = LoggerFactory.getLogger(KbGovernanceDetectorService.class);

    private final KbArticleRepository articleRepo;

    // Detectores por artigo
    private final IncompleteContentDetector incomplete;
    private final InconsistentStructureDetector inconsistent;
    private final OutdatedContentDetector outdated;

    // Detectores globais
    private final DuplicateContentDetector duplicate;

    public KbGovernanceDetectorService(KbArticleRepository articleRepo,
                                       IncompleteContentDetector incomplete,
                                       InconsistentStructureDetector inconsistent,
                                       OutdatedContentDetector outdated,
                                       DuplicateContentDetector duplicate) {
        this.articleRepo = articleRepo;
        this.incomplete = incomplete;
        this.inconsistent = inconsistent;
        this.outdated = outdated;
        this.duplicate = duplicate;
    }

    /**
     * Analisa 1 artigo com todos os detectores por artigo.
     *
     * @param article Artigo a ser analisado
     * @return Quantidade de issues criadas/atualizadas
     */
    @Transactional
    public int analyzeArticle(KbArticle article) {
        if (article == null || article.getId() == null) return 0;

        int issuesCreated = 0;

        // 1) Conteúdo incompleto (vazio/curto/placeholder)
        incomplete.analyze(article);

        // 2) Inconsistente (sem sistema ou em sistema genérico)
        if (inconsistent.analyze(article)) {
            issuesCreated++;
        }

        // 3) Desatualizado (não atualizado há mais de X dias)
        if (outdated.analyze(article)) {
            issuesCreated++;
        }

        return issuesCreated;
    }

    /**
     * Analisa os artigos mais recentes (retorna quantos analisou).
     * Usado no pós-sync para análise rápida.
     *
     * @param limit Quantidade máxima de artigos a analisar
     * @return Quantidade de artigos analisados
     */
    @Transactional
    public int analyzeRecent(int limit) {
        int size = Math.max(1, limit);
        var page = articleRepo.findRecent(PageRequest.of(0, size));

        int totalIssues = 0;
        for (KbArticle article : page) {
            totalIssues += analyzeArticle(article);
        }

        log.info("📊 Análise de {} artigos recentes concluída. Issues criadas/atualizadas: {}",
                page.getNumberOfElements(), totalIssues);

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
