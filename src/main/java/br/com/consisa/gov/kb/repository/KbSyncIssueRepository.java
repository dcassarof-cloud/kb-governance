package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbSyncIssue;
import br.com.consisa.gov.kb.domain.KbSyncIssueType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KbSyncIssueRepository extends JpaRepository<KbSyncIssue, Long> {

    Optional<KbSyncIssue> findByArticleIdAndIssueTypeAndResolvedFalse(Long articleId, KbSyncIssueType issueType);
}
