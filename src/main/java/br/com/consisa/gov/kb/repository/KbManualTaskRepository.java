package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbManualTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface KbManualTaskRepository extends JpaRepository<KbManualTask, Long> {

    interface ManualTaskRow {
        Long getTaskId();
        String getStatus();
        String getRiskLevel();
        String getPriority();
        String getAssigneeType();
        String getAssigneeId();
        Instant getDueAt();
        String getIgnoredReason();
        Long getArticleId();
        String getArticleTitle();
        String getArticleSlug();
        String getArticleUrl();
        String getSystemCode();
        String getSystemName();
        Instant getLastActionAt();
        String getIssueTypes();
    }

    @Query(value = """
        SELECT
          t.id AS taskId,
          t.status AS status,
          t.risk_level AS riskLevel,
          t.priority AS priority,
          t.assignee_type AS assigneeType,
          t.assignee_id AS assigneeId,
          t.due_at AS dueAt,
          t.ignored_reason AS ignoredReason,
          a.id AS articleId,
          a.title AS articleTitle,
          a.slug AS articleSlug,
          a.source_url AS articleUrl,
          COALESCE(s.code,'UNCLASSIFIED') AS systemCode,
          COALESCE(s.name,'Não classificado') AS systemName,
          COALESCE(last_log.last_action_at, t.updated_at) AS lastActionAt,
          issues.issue_types AS issueTypes
        FROM kb_manual_task t
        JOIN kb_article a ON a.id = t.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        LEFT JOIN (
          SELECT task_id, MAX(created_at) AS last_action_at
          FROM kb_manual_action_log
          GROUP BY task_id
        ) last_log ON last_log.task_id = t.id
        LEFT JOIN (
          SELECT article_id, string_agg(DISTINCT issue_type, ',') AS issue_types
          FROM kb_governance_issue
          WHERE status IN ('OPEN','IN_PROGRESS')
          GROUP BY article_id
        ) issues ON issues.article_id = a.id
        WHERE a.article_status = 1
          AND (:systemId IS NULL OR a.system_id = :systemId)
          AND (:status IS NULL OR t.status = :status)
          AND (:risk IS NULL OR t.risk_level = :risk)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:assigneeId IS NULL OR t.assignee_id = :assigneeId)
          AND (:issueType IS NULL OR EXISTS (
                SELECT 1
                FROM kb_governance_issue gi
                WHERE gi.article_id = a.id
                  AND gi.issue_type = :issueType
                  AND gi.status IN ('OPEN','IN_PROGRESS')
          ))
          AND (
                :text IS NULL
                OR LOWER(a.title) LIKE LOWER(CONCAT('%', :text, '%'))
                OR LOWER(a.slug) LIKE LOWER(CONCAT('%', :text, '%'))
                OR LOWER(a.content_text) LIKE LOWER(CONCAT('%', :text, '%'))
          )
        ORDER BY COALESCE(last_log.last_action_at, t.updated_at) DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM kb_manual_task t
        JOIN kb_article a ON a.id = t.article_id
        WHERE a.article_status = 1
          AND (:systemId IS NULL OR a.system_id = :systemId)
          AND (:status IS NULL OR t.status = :status)
          AND (:risk IS NULL OR t.risk_level = :risk)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:assigneeId IS NULL OR t.assignee_id = :assigneeId)
          AND (:issueType IS NULL OR EXISTS (
                SELECT 1
                FROM kb_governance_issue gi
                WHERE gi.article_id = a.id
                  AND gi.issue_type = :issueType
                  AND gi.status IN ('OPEN','IN_PROGRESS')
          ))
          AND (
                :text IS NULL
                OR LOWER(a.title) LIKE LOWER(CONCAT('%', :text, '%'))
                OR LOWER(a.slug) LIKE LOWER(CONCAT('%', :text, '%'))
                OR LOWER(a.content_text) LIKE LOWER(CONCAT('%', :text, '%'))
          )
        """,
            nativeQuery = true)
    Page<ManualTaskRow> pageTasks(
            Pageable pageable,
            @Param("systemId") Long systemId,
            @Param("status") String status,
            @Param("risk") String risk,
            @Param("priority") String priority,
            @Param("assigneeId") String assigneeId,
            @Param("issueType") String issueType,
            @Param("text") String text
    );

    @Query(value = """
        SELECT
          t.id AS taskId,
          t.status AS status,
          t.risk_level AS riskLevel,
          t.priority AS priority,
          t.assignee_type AS assigneeType,
          t.assignee_id AS assigneeId,
          t.due_at AS dueAt,
          t.ignored_reason AS ignoredReason,
          a.id AS articleId,
          a.title AS articleTitle,
          a.slug AS articleSlug,
          a.source_url AS articleUrl,
          COALESCE(s.code,'UNCLASSIFIED') AS systemCode,
          COALESCE(s.name,'Não classificado') AS systemName,
          COALESCE(last_log.last_action_at, t.updated_at) AS lastActionAt,
          issues.issue_types AS issueTypes
        FROM kb_manual_task t
        JOIN kb_article a ON a.id = t.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        LEFT JOIN (
          SELECT task_id, MAX(created_at) AS last_action_at
          FROM kb_manual_action_log
          GROUP BY task_id
        ) last_log ON last_log.task_id = t.id
        LEFT JOIN (
          SELECT article_id, string_agg(DISTINCT issue_type, ',') AS issue_types
          FROM kb_governance_issue
          WHERE status IN ('OPEN','IN_PROGRESS')
          GROUP BY article_id
        ) issues ON issues.article_id = a.id
        WHERE t.id = :taskId
        """, nativeQuery = true)
    ManualTaskRow findTaskRowById(@Param("taskId") Long taskId);

    interface RiskCountRow {
        String getRiskLevel();
        Long getTotal();
    }

    @Query(value = """
        SELECT t.risk_level AS riskLevel, COUNT(*) AS total
        FROM kb_manual_task t
        WHERE t.status <> 'IGNORED'
        GROUP BY t.risk_level
        ORDER BY total DESC
        """, nativeQuery = true)
    List<RiskCountRow> countByRiskLevel();

    interface SlaOverdueRow {
        String getPriority();
        Long getTotal();
    }

    @Query(value = """
        SELECT t.priority AS priority, COUNT(*) AS total
        FROM kb_manual_task t
        WHERE t.due_at IS NOT NULL
          AND t.due_at < NOW()
          AND t.status NOT IN ('DONE','IGNORED')
        GROUP BY t.priority
        ORDER BY total DESC
        """, nativeQuery = true)
    List<SlaOverdueRow> countOverdueByPriority();

    interface CriticalSystemRow {
        String getSystemCode();
        String getSystemName();
        Long getTotal();
    }

    @Query(value = """
        SELECT
          COALESCE(s.code,'UNCLASSIFIED') AS systemCode,
          COALESCE(s.name,'Não classificado') AS systemName,
          COUNT(*) AS total
        FROM kb_manual_task t
        JOIN kb_article a ON a.id = t.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        WHERE t.risk_level IN ('HIGH','CRITICAL')
          AND t.status NOT IN ('DONE','IGNORED')
        GROUP BY COALESCE(s.code,'UNCLASSIFIED'), COALESCE(s.name,'Não classificado')
        ORDER BY total DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<CriticalSystemRow> findTopCriticalSystems(@Param("limit") int limit);
}
