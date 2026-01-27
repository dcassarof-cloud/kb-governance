package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;


import java.util.Optional;

public interface KbGovernanceIssueRepository extends JpaRepository<KbGovernanceIssue, Long> {

    Optional<KbGovernanceIssue> findTop1ByArticleIdAndIssueTypeAndStatusOrderByCreatedAtDesc(
            Long articleId,
            KbGovernanceIssueType issueType,
            GovernanceIssueStatus status
    );
    long countByStatus(GovernanceIssueStatus status);

    long countByStatusAndIssueType(GovernanceIssueStatus status, KbGovernanceIssueType issueType);

    /**
     * Página de issues já “enriquecida” com artigo e sistema (pro front).
     */
    interface IssueRow {
        Long getId();
        String getIssueType();
        String getSeverity();
        String getStatus();
        Long getArticleId();
        String getArticleTitle();
        String getSystemCode();
        String getSystemName();
        String getMessage();
        java.time.OffsetDateTime getCreatedAt();
    }

    @Query(value = """
        SELECT
          i.id                AS id,
          i.issue_type        AS issueType,
          i.severity          AS severity,
          i.status            AS status,
          i.article_id        AS articleId,
          a.title             AS articleTitle,
          COALESCE(s.code,'UNCLASSIFIED') AS systemCode,
          COALESCE(s.name,'Não classificado') AS systemName,
          i.message           AS message,
          i.created_at        AS createdAt
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        WHERE a.article_status = 1
        ORDER BY i.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        WHERE a.article_status = 1
        """,
            nativeQuery = true)
    Page<IssueRow> pageIssues(Pageable pageable);
}
