package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceStatus;
import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 📊 Service central para cálculo de status de governança.
 *
 * REGRA DE NEGÓCIO (Sprint 2):
 * - OK = artigo sem issues abertas (OPEN ou IN_PROGRESS)
 * - WITH_ISSUES = artigo com pelo menos 1 issue aberta
 * - IGNORED = artigo marcado como ignorado (futuro)
 *
 * IMPORTANTE:
 * - Status é calculado on-the-fly (não persistido)
 * - Evita inconsistência entre issues e status
 * - Centraliza a lógica para reuso em dashboard, relatórios, etc.
 */
@Service
public class GovernanceStatusService {

    private final KbGovernanceIssueRepository issueRepo;

    public GovernanceStatusService(KbGovernanceIssueRepository issueRepo) {
        this.issueRepo = issueRepo;
    }

    /**
     * Calcula o status de governança de um artigo.
     *
     * @param articleId ID do artigo
     * @return GovernanceStatus (OK, WITH_ISSUES, IGNORED)
     */
    @Transactional(readOnly = true)
    public GovernanceStatus calculateStatus(Long articleId) {
        if (articleId == null) {
            return GovernanceStatus.OK;
        }

        // Verifica se existe pelo menos 1 issue aberta (OPEN ou IN_PROGRESS)
        boolean hasOpenIssue = issueRepo.existsByArticleIdAndStatusIn(
                articleId,
                List.of(GovernanceIssueStatus.OPEN, GovernanceIssueStatus.IN_PROGRESS)
        );

        return hasOpenIssue ? GovernanceStatus.WITH_ISSUES : GovernanceStatus.OK;
    }

    /**
     * Calcula o status de governança para múltiplos artigos (batch).
     * Otimizado para evitar N+1 queries.
     *
     * @param articleIds Lista de IDs dos artigos
     * @return Map de articleId → GovernanceStatus
     */
    @Transactional(readOnly = true)
    public Map<Long, GovernanceStatus> calculateStatusBatch(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }

        // Busca todos os artigos que têm issues abertas (OPEN ou IN_PROGRESS)
        Set<Long> articlesWithOpenIssues = issueRepo.findArticleIdsWithOpenIssues(
                List.of(GovernanceIssueStatus.OPEN, GovernanceIssueStatus.IN_PROGRESS)
        );

        // Monta o mapa de status
        return articleIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> articlesWithOpenIssues.contains(id)
                                ? GovernanceStatus.WITH_ISSUES
                                : GovernanceStatus.OK
                ));
    }

    /**
     * Calcula o status de governança para um artigo (objeto).
     *
     * @param article Entidade KbArticle
     * @return GovernanceStatus
     */
    public GovernanceStatus calculateStatus(KbArticle article) {
        if (article == null || article.getId() == null) {
            return GovernanceStatus.OK;
        }
        return calculateStatus(article.getId());
    }
}
