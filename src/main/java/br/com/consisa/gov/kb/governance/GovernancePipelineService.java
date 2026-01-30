package br.com.consisa.gov.kb.governance;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquestrador dos detectores de governança.
 */
@Service
public class GovernancePipelineService {

    private static final Logger log = LoggerFactory.getLogger(GovernancePipelineService.class);

    private final KbArticleRepository articleRepository;
    private final List<KbGovernanceDetector> detectors;

    public GovernancePipelineService(
            KbArticleRepository articleRepository,
            List<KbGovernanceDetector> detectors
    ) {
        this.articleRepository = articleRepository;
        this.detectors = detectors;
    }

    @Transactional
    public void analyzeArticle(KbArticle article) {
        if (article == null || article.getId() == null) {
            return;
        }

        for (KbGovernanceDetector detector : detectors) {
            detector.analyze(article);
        }
    }

    @Transactional
    public int analyzeRecent(int limit) {
        int size = Math.max(1, limit);
        var page = articleRepository.findRecent(PageRequest.of(0, size));

        page.forEach(this::analyzeArticle);

        log.info("📊 Pipeline: {} artigos analisados com {} detectores.",
                page.getNumberOfElements(), detectors.size());

        return page.getNumberOfElements();
    }
}
