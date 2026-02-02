package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbGovernanceIssueAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface KbGovernanceIssueAssignmentRepository extends JpaRepository<KbGovernanceIssueAssignment, Long> {
    Optional<KbGovernanceIssueAssignment> findTop1ByIssueIdOrderByCreatedAtDesc(Long issueId);

    interface PendingByAgentRow {
        String getAgentId();
        String getAgentName();
        Long getPendingIssues();
    }

    @Query(value = """
        SELECT
          last_assign.agent_id AS agentId,
          last_assign.agent_name AS agentName,
          COUNT(*) AS pendingIssues
        FROM kb_governance_issue i
        JOIN LATERAL (
            SELECT ia.agent_id, ia.agent_name
            FROM kb_governance_issue_assignment ia
            WHERE ia.issue_id = i.id
            ORDER BY ia.created_at DESC
            LIMIT 1
        ) last_assign ON true
        WHERE i.status NOT IN ('RESOLVED', 'IGNORED')
          AND last_assign.agent_id IS NOT NULL
        GROUP BY last_assign.agent_id, last_assign.agent_name
        """, nativeQuery = true)
    List<PendingByAgentRow> countPendingIssuesByAgent();

    interface ResponsibleSummaryRow {
        String getAgentId();
        String getAgentName();
        Long getOpenIssues();
        Long getOverdueIssues();
        OffsetDateTime getLastAssignedAt();
        Double getAvgResolutionDays();
    }

    @Query(value = """
        SELECT
          last_assign.agent_id AS agentId,
          last_assign.agent_name AS agentName,
          COUNT(*) FILTER (
            WHERE i.status NOT IN ('RESOLVED', 'IGNORED')
          ) AS openIssues,
          COUNT(*) FILTER (
            WHERE i.status NOT IN ('RESOLVED', 'IGNORED')
              AND last_assign.due_date IS NOT NULL
              AND last_assign.due_date < NOW()
          ) AS overdueIssues,
          MAX(last_assign.assigned_at) AS lastAssignedAt,
          AVG(EXTRACT(EPOCH FROM (i.resolved_at - i.created_at)) / 86400.0)
            FILTER (WHERE i.resolved_at IS NOT NULL) AS avgResolutionDays
        FROM kb_governance_issue i
        JOIN LATERAL (
            SELECT ia.agent_id, ia.agent_name, ia.assigned_at, ia.due_date
            FROM kb_governance_issue_assignment ia
            WHERE ia.issue_id = i.id
            ORDER BY ia.created_at DESC
            LIMIT 1
        ) last_assign ON true
        WHERE last_assign.agent_id IS NOT NULL
        GROUP BY last_assign.agent_id, last_assign.agent_name
        """, nativeQuery = true)
    List<ResponsibleSummaryRow> summarizeByResponsible();
}
