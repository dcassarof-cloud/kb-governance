package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbSyncIssue;
import br.com.consisa.gov.kb.domain.KbSyncIssueType;
import br.com.consisa.gov.kb.repository.KbSyncIssueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class KbSyncIssueService {

    private final KbSyncIssueRepository repo;

    public KbSyncIssueService(KbSyncIssueRepository repo) {
        this.repo = repo;
    }

    /**
     * Abre uma issue (se já existir aberta, apenas atualiza a mensagem).
     * Isso evita spam e mantém "a última causa" registrada.
     */
    @Transactional
    public void open(Long articleId, KbSyncIssueType type, String message) {
        if (articleId == null) return;

        String msg = truncate(message, 400);

        repo.findByArticleIdAndIssueTypeAndResolvedFalse(articleId, type)
                .ifPresentOrElse(existing -> {
                    // Atualiza mensagem para refletir o último cenário observado
                    existing.setMessage(msg);
                    repo.save(existing);
                }, () -> {
                    KbSyncIssue issue = new KbSyncIssue();
                    issue.setArticleId(articleId);
                    issue.setIssueType(type);
                    issue.setMessage(msg);
                    issue.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    issue.setResolved(false);
                    repo.save(issue);
                });
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}
