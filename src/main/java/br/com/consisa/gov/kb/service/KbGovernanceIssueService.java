package br.com.consisa.gov.kb.service;

import com.fasterxml.jackson.databind.JsonNode;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.GovernanceSeverity;
import br.com.consisa.gov.kb.domain.KbGovernanceIssue;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import br.com.consisa.gov.kb.repository.KbGovernanceIssueRepository;
import br.com.consisa.gov.kb.util.DateTimeUtils;
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
    private final GovernanceSlaService slaService;
    private final KbGovernanceIssueHistoryService historyService;

    public KbGovernanceIssueService(
            KbGovernanceIssueRepository repo,
            GovernanceSlaService slaService,
            KbGovernanceIssueHistoryService historyService
    ) {
        this.repo = repo;
        this.slaService = slaService;
        this.historyService = historyService;
    }

    @Transactional
    public KbGovernanceIssue open(Long articleId,
                                  KbGovernanceIssueType type,
                                  GovernanceSeverity severity,
                                  String message,
                                  JsonNode evidence) {

        var existing = repo.findTop1ByArticleIdAndIssueTypeOrderByCreatedAtDesc(articleId, type);

        KbGovernanceIssue issue = existing.orElseGet(KbGovernanceIssue::new);
        boolean isNew = issue.getId() == null;
        GovernanceIssueStatus previousStatus = issue.getStatus();
        KbGovernanceIssue beforeChange = snapshot(issue);
        issue.setArticleId(articleId);
        issue.setIssueType(type);
        if (previousStatus == GovernanceIssueStatus.RESOLVED || previousStatus == GovernanceIssueStatus.IGNORED) {
            issue.setStatus(GovernanceIssueStatus.OPEN);
            issue.setResolvedAt(null);
            issue.setResolvedBy(null);
            issue.setIgnoredReason(null);
            issue.setSlaDueAt(slaService.calculateReopenedSlaDueAt(severity));
        } else if (previousStatus == null) {
            issue.setStatus(GovernanceIssueStatus.OPEN);
        }
        issue.setSeverity(severity);
        issue.setMessage(trunc(message, 400));
        issue.setEvidence(evidence);
        if (issue.getSlaDueAt() == null) {
            issue.setSlaDueAt(slaService.calculateDueAt(DateTimeUtils.nowSaoPaulo(), severity));
        }

        KbGovernanceIssue saved = repo.save(issue);
        if (isNew) {
            historyService.recordCreated(saved, "system");
        } else if (previousStatus == GovernanceIssueStatus.RESOLVED || previousStatus == GovernanceIssueStatus.IGNORED) {
            historyService.recordReopened(beforeChange, saved, "system");
            historyService.recordStatusChanged(beforeChange, saved, "system");
        }
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

    private KbGovernanceIssue snapshot(KbGovernanceIssue issue) {
        if (issue == null) {
            return null;
        }
        KbGovernanceIssue copy = new KbGovernanceIssue();
        copy.setStatus(issue.getStatus());
        copy.setSlaDueAt(issue.getSlaDueAt());
        copy.setResponsibleId(issue.getResponsibleId());
        copy.setResponsibleType(issue.getResponsibleType());
        return copy;
    }
}
