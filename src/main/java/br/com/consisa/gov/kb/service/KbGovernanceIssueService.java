package br.com.consisa.gov.kb.service;

import com.fasterxml.jackson.databind.JsonNode;
import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para abrir/atualizar issues de governança (sem duplicar issue OPEN do mesmo tipo).
 */
@Service
public class KbGovernanceIssueService {

    private final KbGovernanceIssueRepository repo;

    public KbGovernanceIssueService(KbGovernanceIssueRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public KbGovernanceIssue open(Long articleId,
                                  KbGovernanceIssueType type,
                                  GovernanceSeverity severity,
                                  String message,
                                  JsonNode evidence) {

        var existing = repo.findTop1ByArticleIdAndIssueTypeAndStatusOrderByCreatedAtDesc(
                articleId, type, GovernanceIssueStatus.OPEN
        );

        KbGovernanceIssue issue = existing.orElseGet(KbGovernanceIssue::new);
        issue.setArticleId(articleId);
        issue.setIssueType(type);
        issue.setStatus(GovernanceIssueStatus.OPEN);
        issue.setSeverity(severity);
        issue.setMessage(trunc(message, 400));
        issue.setEvidence(evidence);

        return repo.save(issue);
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
