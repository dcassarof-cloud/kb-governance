package br.com.consisa.gov.kb.service;

import com.fasterxml.jackson.databind.JsonNode;
import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para abrir/atualizar issues de governança (sem duplicar issue OPEN do mesmo tipo).
 */
@Service
public class KbGovernanceIssueService {

    private static final Logger log = LoggerFactory.getLogger(KbGovernanceIssueService.class);

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

        var existing = repo.findTop1ByArticleIdAndIssueTypeOrderByCreatedAtDesc(articleId, type);

        KbGovernanceIssue issue = existing.orElseGet(KbGovernanceIssue::new);
        GovernanceIssueStatus previousStatus = issue.getStatus();
        issue.setArticleId(articleId);
        issue.setIssueType(type);
        if (previousStatus == GovernanceIssueStatus.RESOLVED || previousStatus == GovernanceIssueStatus.IGNORED) {
            issue.setStatus(GovernanceIssueStatus.OPEN);
            issue.setResolvedAt(null);
            issue.setResolvedBy(null);
            issue.setIgnoredReason(null);
        } else if (previousStatus == null) {
            issue.setStatus(GovernanceIssueStatus.OPEN);
        }
        issue.setSeverity(severity);
        issue.setMessage(trunc(message, 400));
        issue.setEvidence(evidence);

        KbGovernanceIssue saved = repo.save(issue);
        if (previousStatus != null && previousStatus != saved.getStatus()) {
            log.info("🔁 Issue reaberta: articleId={} type={} status {} -> {}",
                    articleId, type, previousStatus, saved.getStatus());
        }
        return saved;
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
