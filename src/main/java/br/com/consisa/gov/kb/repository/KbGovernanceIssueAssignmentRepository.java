package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbGovernanceIssueAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KbGovernanceIssueAssignmentRepository extends JpaRepository<KbGovernanceIssueAssignment, Long> {
    Optional<KbGovernanceIssueAssignment> findTop1ByIssueIdOrderByCreatedAtDesc(Long issueId);
}
