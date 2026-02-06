package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.repository.projection.OverdueIssue;
import br.com.consisa.gov.kb.repository.projection.SlaComplianceTotals;
import br.com.consisa.gov.kb.repository.projection.TrendsTotals;
import br.com.consisa.gov.kb.repository.projection.UnassignedIssue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class GovernanceDashboardRepository {

    private static final Logger log = LoggerFactory.getLogger(GovernanceDashboardRepository.class);

    private final EntityManager entityManager;

    public GovernanceDashboardRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public SlaComplianceTotals fetchSlaComplianceTotals() {
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
            """, Tuple.class);
        List<Tuple> rows = query.getResultList();
        Tuple row = rows.isEmpty() ? null : rows.get(0);
        return new SlaComplianceTotals(
                toLong(row, "total_resolved"),
                toLong(row, "resolved_on_time")
        );
    }

    @SuppressWarnings("unchecked")
    public List<OverdueIssue> fetchOverdueIssues(int limit) {
        Query query = entityManager.createNativeQuery("""
            SELECT
                i.id AS id,
                i.issue_type AS issue_type,
                COALESCE(s.code, 'UNCLASSIFIED') AS system_code,
                COALESCE(s.name, 'Não classificado') AS system_name,
                i.severity AS severity,
                i.sla_due_at AS sla_due_at,
                i.created_at AS created_at
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
            """, Tuple.class);
        query.setMaxResults(limit);
        List<Tuple> rows = query.getResultList();
        return rows.stream()
                .map(row -> new OverdueIssue(
                        toLong(row, "id"),
                        asString(row, "issue_type"),
                        asString(row, "system_code"),
                        asString(row, "system_name"),
                        asString(row, "severity"),
                        toOffsetDateTime(row, "sla_due_at"),
                        toOffsetDateTime(row, "created_at")
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<UnassignedIssue> fetchUnassignedIssues(int limit) {
        Query query = entityManager.createNativeQuery("""
            SELECT
                i.id AS id,
                i.issue_type AS issue_type,
                COALESCE(s.code, 'UNCLASSIFIED') AS system_code,
                COALESCE(s.name, 'Não classificado') AS system_name,
                i.severity AS severity,
                i.created_at AS created_at
            FROM kb_governance_issue i
            JOIN kb_article a ON a.id = i.article_id
            LEFT JOIN kb_system s ON s.id = a.system_id
            WHERE a.article_status = 1
              AND i.status NOT IN ('RESOLVED', 'IGNORED')
              AND i.responsible_id IS NULL
            ORDER BY i.created_at DESC, i.id
            """, Tuple.class);
        query.setMaxResults(limit);
        List<Tuple> rows = query.getResultList();
        return rows.stream()
                .map(row -> new UnassignedIssue(
                        toLong(row, "id"),
                        asString(row, "issue_type"),
                        asString(row, "system_code"),
                        asString(row, "system_name"),
                        asString(row, "severity"),
                        toOffsetDateTime(row, "created_at")
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public TrendsTotals fetchTrendsTotals() {
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
            """, Tuple.class);
        List<Tuple> rows = query.getResultList();
        Tuple row = rows.isEmpty() ? null : rows.get(0);
        return new TrendsTotals(
                toLong(row, "opened_last_7"),
                toLong(row, "resolved_last_7"),
                toLong(row, "overdue_last_7"),
                toLong(row, "overdue_prev_7")
        );
    }

    private long toLong(Tuple row, String alias) {
        if (row == null) {
            return 0L;
        }
        Object value = row.get(alias);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String asString(Tuple row, String alias) {
        if (row == null) {
            return null;
        }
        Object value = row.get(alias);
        return value == null ? null : value.toString();
    }

    private OffsetDateTime toOffsetDateTime(Tuple row, String alias) {
        if (row == null) {
            return null;
        }
        Object value = row.get(alias);
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof java.time.Instant instant) {
            return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return localDateTime.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
        }
        if (value instanceof java.util.Date date) {
            return OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
        }
        log.warn("Valor de data inesperado para alias '{}': {}", alias, value.getClass().getName());
        return null;
    }
}
