package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KbGovernanceIssueRepository extends JpaRepository<KbGovernanceIssue, Long> {

    Optional<KbGovernanceIssue> findTop1ByArticleIdAndIssueTypeAndStatusOrderByCreatedAtDesc(
            Long articleId,
            KbGovernanceIssueType issueType,
            GovernanceIssueStatus status
    );
}
