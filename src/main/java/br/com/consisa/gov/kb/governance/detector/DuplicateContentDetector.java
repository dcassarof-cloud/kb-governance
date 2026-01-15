package br.com.consisa.gov.kb.governance.detector;

import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.service.KbGovernanceIssueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * Analisa TODOS os hashes duplicados
     */
    @Transactional
    public void analyzeAllDuplicates() {
        List<String> hashes = articleRepo.findDuplicateContentHashes();
        hashes.forEach(this::analyzeHash);
    }

    /**
     * Analisa um hash específico
     */
    @Transactional
    public void analyzeHash(String hash) {
        List<Long> ids = articleRepo.findArticleIdsByContentHash(hash);

        if (ids.size() < 2) return;

        ObjectNode evidence = mapper.createObjectNode();
        evidence.put("hash", hash);
        evidence.put("count", ids.size());
        evidence.putPOJO("articleIds", ids);

        for (Long id : ids) {
            issueService.open(
                    id,
                    KbGovernanceIssueType.DUPLICATE_CONTENT,
                    GovernanceSeverity.WARN,
                    "Conteúdo duplicado detectado (mesmo hash)",
                    evidence
            );
        }
    }
}
