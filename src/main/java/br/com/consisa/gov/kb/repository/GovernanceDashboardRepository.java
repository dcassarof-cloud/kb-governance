package br.com.consisa.gov.kb.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GovernanceDashboardRepository {

    private final EntityManager entityManager;

    public GovernanceDashboardRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public Object[] fetchSlaComplianceTotals() {
        Query query = entityManager.createNativeQuery("""
            SELECT
                COUNT(*) FILTER (WHERE status = 'RESOLVED' AND resolved_at IS NOT NULL) AS total_resolved,
                COUNT(*) FILTER (
                    WHERE status = 'RESOLVED'
                      AND resolved_at IS NOT NULL
                      AND sla_due_at IS NOT NULL
                      AND resolved_at <= sla_due_at
                ) AS resolved_on_time
            FROM kb_governance_issue
            """);
        List<Object[]> rows = query.getResultList();
        return rows.isEmpty() ? new Object[2] : rows.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> fetchOverdueIssues(int limit) {
        Query query = entityManager.createNativeQuery("""
            SELECT
                i.id,
                i.issue_type,
                COALESCE(s.code, 'UNCLASSIFIED') AS system_code,
                COALESCE(s.name, 'Não classificado') AS system_name,
                i.severity,
                i.sla_due_at
            FROM kb_governance_issue i
            JOIN kb_article a ON a.id = i.article_id
            LEFT JOIN kb_system s ON s.id = a.system_id
            WHERE a.article_status = 1
              AND i.status NOT IN ('RESOLVED', 'IGNORED')
              AND i.sla_due_at IS NOT NULL
              AND i.sla_due_at < NOW()
            ORDER BY
              i.sla_due_at ASC,
              CASE i.severity
                WHEN 'ERROR' THEN 3
                WHEN 'WARN' THEN 2
                WHEN 'INFO' THEN 1
                ELSE 0
              END DESC,
              i.id
            """);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> fetchUnassignedIssues(int limit) {
        Query query = entityManager.createNativeQuery("""
            SELECT
                i.id,
                i.issue_type,
                COALESCE(s.code, 'UNCLASSIFIED') AS system_code,
                COALESCE(s.name, 'Não classificado') AS system_name,
                i.severity,
                i.created_at
            FROM kb_governance_issue i
            JOIN kb_article a ON a.id = i.article_id
            LEFT JOIN kb_system s ON s.id = a.system_id
            WHERE a.article_status = 1
              AND i.status NOT IN ('RESOLVED', 'IGNORED')
              AND i.responsible_id IS NULL
            ORDER BY i.created_at DESC, i.id
            """);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public Object[] fetchTrendsTotals() {
        Query query = entityManager.createNativeQuery("""
            SELECT
                COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '7 days') AS opened_last_7,
                COUNT(*) FILTER (WHERE resolved_at IS NOT NULL AND resolved_at >= NOW() - INTERVAL '7 days') AS resolved_last_7,
                COUNT(*) FILTER (
                    WHERE status NOT IN ('RESOLVED', 'IGNORED')
                      AND sla_due_at IS NOT NULL
                      AND sla_due_at < NOW()
                      AND sla_due_at >= NOW() - INTERVAL '7 days'
                ) AS overdue_last_7,
                COUNT(*) FILTER (
                    WHERE status NOT IN ('RESOLVED', 'IGNORED')
                      AND sla_due_at IS NOT NULL
                      AND sla_due_at < NOW() - INTERVAL '7 days'
                      AND sla_due_at >= NOW() - INTERVAL '14 days'
                ) AS overdue_prev_7
            FROM kb_governance_issue
            """);
        List<Object[]> rows = query.getResultList();
        return rows.isEmpty() ? new Object[4] : rows.get(0);
    }
}
