package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbGovernanceIssueHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KbGovernanceIssueHistoryRepository extends JpaRepository<KbGovernanceIssueHistory, Long> {
    List<KbGovernanceIssueHistory> findByIssueIdOrderByCreatedAtAsc(Long issueId);
}
