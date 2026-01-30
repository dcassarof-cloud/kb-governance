package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.governance.KbGovernanceDetector;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.service.KbGovernanceIssueService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DuplicateContentDetector implements KbGovernanceDetector {

    private static final Logger log = LoggerFactory.getLogger(DuplicateContentDetector.class);

    private final KbArticleRepository articleRepo;
    private final KbGovernanceIssueService issueService;

    public DuplicateContentDetector(KbArticleRepository articleRepo,
                                    KbGovernanceIssueService issueService) {
        this.articleRepo = articleRepo;
        this.issueService = issueService;
    }

    @Override
    public void analyze(KbArticle article) {
        if (article == null || article.getContentHash() == null) {
            return;
        }
        analyzeHash(article.getContentHash());
    }

    /**
     * Analisa todos os hashes duplicados e abre issues.
     * @return quantidade de issues abertas/atualizadas
     */
    @Transactional
    public int analyzeAllDuplicates() {
        final List<String> hashes = articleRepo.findDuplicateHashes();
        int opened = 0;

        for (String hash : hashes) {
            opened += analyzeHash(hash);
        }

        log.info("🧬 DUPLICATE_CONTENT: hashesDuplicados={} issuesAbertas={}", hashes.size(), opened);
        return opened;
    }

    /**
     * Analisa um hash específico e abre issues para os artigos envolvidos.
     * @return quantidade de issues abertas/atualizadas
     */
    @Transactional
    public int analyzeHash(String hash) {
        if (hash == null || hash.isBlank()) return 0;
        if ("N/A".equalsIgnoreCase(hash.trim())) return 0;

        final List<KbArticle> group = articleRepo.findByContentHashOrderByUpdatedDateDesc(hash);
        if (group == null || group.size() < 2) return 0;

        // evidence JSON (sem ObjectMapper)
        final ObjectNode evidence = JsonNodeFactory.instance.objectNode();
        evidence.put("hash", hash);
        evidence.put("count", group.size());

        final ArrayNode ids = evidence.putArray("articleIds");
        final ArrayNode titles = evidence.putArray("titles");

        for (KbArticle a : group) {
            ids.add(a.getId());
            titles.add(a.getTitle() == null ? "" : a.getTitle());
        }

        /**
         * Abre issue para cada artigo do grupo.
         * Motivo: rastreabilidade por artigo (cada manual fica "marcado" no painel).
         * Alternativa futura: abrir 1 issue "grupo" e linkar os artigos.
         */
        int opened = 0;
        for (KbArticle a : group) {
            String msg = "Conteúdo duplicado detectado. hash=" + hash + " (grupo=" + group.size() + ")";
            issueService.open(
                    a.getId(),
                    KbGovernanceIssueType.DUPLICATE_CONTENT,
                    GovernanceSeverity.WARN,
                    msg,
                    evidence
            );
            opened++;
        }

        log.warn("🧬 DUPLICATE_CONTENT hash={} count={} (issues={})", hash, group.size(), opened);
        return opened;
    }
}
