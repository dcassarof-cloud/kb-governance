package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.service.KbGovernanceIssueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Detector de conteúdo duplicado.
 *
 * Estratégia:
 * - Usa content_hash (SHA-256) previamente calculado em kb_article.content_hash
 * - Encontra hashes repetidos
 * - Abre issue DUPLICATE_CONTENT para cada artigo do grupo
 *
 * Observação:
 * - É mais prático abrir issue em TODOS os artigos do grupo,
 *   porque qualquer um pode aparecer na busca/tela.
 */
@Component
public class DuplicateContentDetector {

    private final KbArticleRepository articleRepo;
    private final KbGovernanceIssueService issueService;
    private final ObjectMapper mapper;

    public DuplicateContentDetector(
            KbArticleRepository articleRepo,
            KbGovernanceIssueService issueService,
            ObjectMapper mapper
    ) {
        this.articleRepo = articleRepo;
        this.issueService = issueService;
        this.mapper = mapper;
    }

    /**
     * Analisa todos os hashes duplicados no banco.
     *
     * @return quantidade de issues (re)abertas/atualizadas
     */
    @Transactional
    public int analyzeAllDuplicates() {
        List<String> hashes = articleRepo.findDuplicateContentHashes();
        int total = 0;

        for (String hash : hashes) {
            total += analyzeHash(hash);
        }
        return total;
    }

    /**
     * Analisa um hash específico.
     *
     * @param hash content_hash (sha-256 hex)
     * @return quantidade de issues (re)abertas/atualizadas
     */
    @Transactional
    public int analyzeHash(String hash) {
        if (hash == null || hash.isBlank()) return 0;

        // IDs do grupo duplicado
        List<Long> ids = articleRepo.findArticleIdsByContentHash(hash);

        // se não tiver duplicidade real, não faz nada
        if (ids == null || ids.size() <= 1) return 0;

        // evidence JSON (vai para coluna jsonb)
        ObjectNode evidence = mapper.createObjectNode();
        evidence.put("hash", hash);
        evidence.put("count", ids.size());

        ArrayNode arr = evidence.putArray("articleIds");
        for (Long id : ids) arr.add(id);

        String msg = "Conteúdo duplicado detectado (mesmo content_hash). " +
                "Hash=" + hash + " | ids=" + ids;

        int opened = 0;

        // abre/atualiza issue para cada artigo do grupo
        for (Long articleId : ids) {
            issueService.open(
                    articleId,
                    KbGovernanceIssueType.DUPLICATE_CONTENT,
                    GovernanceSeverity.MEDIUM,
                    msg,
                    evidence
            );
            opened++;
        }

        return opened;
    }
}
