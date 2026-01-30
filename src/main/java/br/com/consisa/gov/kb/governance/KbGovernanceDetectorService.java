package br.com.consisa.gov.kb.governance;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.governance.detector.DuplicateContentDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final GovernancePipelineService pipelineService;
    private final DuplicateContentDetector duplicate;

    public KbGovernanceDetectorService(
            GovernancePipelineService pipelineService,
            DuplicateContentDetector duplicate
    ) {
        this.pipelineService = pipelineService;
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
        pipelineService.analyzeArticle(article);
        return 0;
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
        int analyzed = pipelineService.analyzeRecent(limit);
        log.info("📊 Análise de {} artigos recentes concluída via pipeline.", analyzed);
        return analyzed;
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
