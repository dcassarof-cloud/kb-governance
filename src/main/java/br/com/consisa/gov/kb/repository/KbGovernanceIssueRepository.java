package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import br.com.consisa.gov.kb.domain.GovernanceIssueStatus;
import br.com.consisa.gov.kb.domain.KbGovernanceIssueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;


import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface KbGovernanceIssueRepository extends JpaRepository<KbGovernanceIssue, Long> {

    Optional<KbGovernanceIssue> findTop1ByArticleIdAndIssueTypeAndStatusOrderByCreatedAtDesc(
            Long articleId,
            KbGovernanceIssueType issueType,
            GovernanceIssueStatus status
    );

    Optional<KbGovernanceIssue> findTop1ByArticleIdAndIssueTypeOrderByCreatedAtDesc(
            Long articleId,
            KbGovernanceIssueType issueType
    );
    long countByStatus(GovernanceIssueStatus status);

    long countByStatusAndIssueType(GovernanceIssueStatus status, KbGovernanceIssueType issueType);

    /**
     * Verifica se existe issue aberta para um artigo.
     * Usado pelo GovernanceStatusService para calcular status.
     */
    boolean existsByArticleIdAndStatusIn(Long articleId, List<GovernanceIssueStatus> statuses);

    /**
     * Busca IDs de artigos com issues abertas (batch query para evitar N+1).
     */
    @Query("SELECT DISTINCT i.articleId FROM KbGovernanceIssue i WHERE i.status IN :statuses")
    Set<Long> findArticleIdsWithOpenIssues(@org.springframework.data.repository.query.Param("statuses") List<GovernanceIssueStatus> statuses);

    /**
     * 📊 Conta artigos DISTINTOS com issues abertas (OPEN, ASSIGNED ou IN_PROGRESS).
     * Usado para calcular: OK = totalArtigos - artigosComIssueAberta
     *
     * REGRA DE NEGÓCIO (Sprint 1):
     * - "Issue aberta" = status OPEN, ASSIGNED ou IN_PROGRESS
     * - Quando analista assume issue (IN_PROGRESS), continua sendo problema aberto
     * - Um artigo com múltiplas issues abertas conta só uma vez
     * - Só deixa de contar quando TODAS as issues do artigo são RESOLVED
     */
    @Query("SELECT COUNT(DISTINCT i.articleId) FROM KbGovernanceIssue i " +
           "WHERE i.status NOT IN (br.com.consisa.gov.kb.domain.GovernanceIssueStatus.RESOLVED, " +
           "br.com.consisa.gov.kb.domain.GovernanceIssueStatus.IGNORED)")
    long countDistinctArticlesWithOpenIssues();

    /**
     * 📊 Conta total de issues abertas (OPEN, ASSIGNED ou IN_PROGRESS).
     *
     * REGRA DE NEGÓCIO (Sprint 1):
     * - "Issue aberta" = OPEN, ASSIGNED ou IN_PROGRESS
     * - Issue em tratamento (IN_PROGRESS) ainda é problema aberto
     * - Só fecha quando status = RESOLVED
     */
    @Query("SELECT COUNT(i) FROM KbGovernanceIssue i " +
           "WHERE i.status NOT IN (br.com.consisa.gov.kb.domain.GovernanceIssueStatus.RESOLVED, " +
           "br.com.consisa.gov.kb.domain.GovernanceIssueStatus.IGNORED)")
    long countOpenIssues();

    @Query("SELECT COUNT(DISTINCT i.articleId) FROM KbGovernanceIssue i")
    long countDistinctArticlesWithIssues();

    @Query("SELECT COUNT(i) FROM KbGovernanceIssue i")
    long countTotalIssues();

    /**
     * Página de issues já "enriquecida" com artigo e sistema (pro front).
     *
     * IMPORTANTE: createdAt retorna java.time.Instant porque PostgreSQL TIMESTAMPTZ
     * é mapeado para Instant pelo JDBC. A conversão para OffsetDateTime é feita no mapper.
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
        java.time.Instant getCreatedAt();  // PostgreSQL TIMESTAMPTZ → Instant
        java.time.Instant getUpdatedAt();
        String getAssignedAgentId();
        String getAssignedAgentName();
        java.time.Instant getDueDate();
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
          i.created_at        AS createdAt,
          i.updated_at        AS updatedAt,
          last_assign.agent_id AS assignedAgentId,
          last_assign.agent_name AS assignedAgentName,
          last_assign.due_date AS dueDate
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        LEFT JOIN LATERAL (
            SELECT ia.agent_id, ia.agent_name, ia.due_date
            FROM kb_governance_issue_assignment ia
            WHERE ia.issue_id = i.id
            ORDER BY ia.created_at DESC
            LIMIT 1
        ) last_assign ON true
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

    /**
     * 📋 Página de issues COM FILTROS (type e/ou status).
     * Filtros são opcionais - passando null ou vazio ignora o filtro.
     */
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
          i.created_at        AS createdAt,
          i.updated_at        AS updatedAt,
          last_assign.agent_id AS assignedAgentId,
          last_assign.agent_name AS assignedAgentName,
          last_assign.due_date AS dueDate
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        LEFT JOIN LATERAL (
            SELECT ia.agent_id, ia.agent_name
            FROM kb_governance_issue_assignment ia
            WHERE ia.issue_id = i.id
            ORDER BY ia.created_at DESC
            LIMIT 1
        ) last_assign ON true
        WHERE a.article_status = 1
          AND (:issueType IS NULL OR i.issue_type = :issueType)
          AND (:severity IS NULL OR i.severity = :severity)
          AND (:status IS NULL OR i.status = :status)
          AND (:systemCode IS NULL OR s.code = :systemCode)
          AND (
              :responsible IS NULL
              OR last_assign.agent_id = :responsible
              OR lower(last_assign.agent_name) LIKE lower(concat('%', :responsible, '%'))
          )
        ORDER BY i.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        LEFT JOIN LATERAL (
            SELECT ia.agent_id, ia.agent_name
            FROM kb_governance_issue_assignment ia
            WHERE ia.issue_id = i.id
            ORDER BY ia.created_at DESC
            LIMIT 1
        ) last_assign ON true
        WHERE a.article_status = 1
          AND (:issueType IS NULL OR i.issue_type = :issueType)
          AND (:severity IS NULL OR i.severity = :severity)
          AND (:status IS NULL OR i.status = :status)
          AND (:systemCode IS NULL OR s.code = :systemCode)
          AND (
              :responsible IS NULL
              OR last_assign.agent_id = :responsible
              OR lower(last_assign.agent_name) LIKE lower(concat('%', :responsible, '%'))
          )
        """,
            nativeQuery = true)
    Page<IssueRow> pageIssuesFiltered(
            Pageable pageable,
            @org.springframework.data.repository.query.Param("issueType") String issueType,
            @org.springframework.data.repository.query.Param("severity") String severity,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("systemCode") String systemCode,
            @org.springframework.data.repository.query.Param("responsible") String responsible
    );

    interface IssueTypeCountRow {
        KbGovernanceIssueType getIssueType();
        Long getTotal();
    }

    @Query("SELECT i.issueType AS issueType, COUNT(i) AS total FROM KbGovernanceIssue i GROUP BY i.issueType")
    List<IssueTypeCountRow> countByIssueType();

    @Query(value = """
        SELECT i.id
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        WHERE i.issue_type = 'DUPLICATE_CONTENT'
          AND a.content_hash = :hash
        """, nativeQuery = true)
    List<Long> findDuplicateIssueIdsByHash(@org.springframework.data.repository.query.Param("hash") String hash);

    @Query(value = """
        SELECT i.status
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        WHERE i.issue_type = 'DUPLICATE_CONTENT'
          AND a.content_hash = :hash
        """, nativeQuery = true)
    List<String> findDuplicateIssueStatusesByHash(@org.springframework.data.repository.query.Param("hash") String hash);

    // ========================================
    // SPRINT 5: Overview Queries
    // ========================================

    /**
     * Resultado de totais agregados para overview.
     */
    interface OverviewTotalsRow {
        long getOpenCount();
        long getCriticalOpenCount();
        long getUnassignedCount();
        long getOverdueCount();
    }

    /**
     * Resultado de overview por sistema.
     */
    interface OverviewBySystemRow {
        String getSystemCode();
        String getSystemName();
        long getOpenCount();
        long getCriticalCount();
        long getOverdueCount();
        long getUnassignedCount();
        long getErrorCount();
        long getWarnCount();
        long getInfoCount();
    }

    /**
     * Busca totais agregados para overview gerencial.
     */
    @Query(value = """
        SELECT
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED')) AS openCount,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'ERROR') AS criticalOpenCount,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.responsible_id IS NULL) AS unassignedCount,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.sla_due_at < NOW()) AS overdueCount
        FROM kb_governance_issue i
        """, nativeQuery = true)
    OverviewTotalsRow fetchOverviewTotals();

    /**
     * Busca overview agrupado por sistema.
     */
    @Query(value = """
        SELECT
            COALESCE(s.code, 'UNCLASSIFIED') AS systemCode,
            COALESCE(s.name, 'Não classificado') AS systemName,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED')) AS openCount,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'ERROR') AS criticalCount,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.sla_due_at < NOW()) AS overdueCount,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.responsible_id IS NULL) AS unassignedCount,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'ERROR') AS errorCount,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'WARN') AS warnCount,
            COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'INFO') AS infoCount
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        WHERE a.article_status = 1
        GROUP BY s.code, s.name
        ORDER BY openCount DESC
        """, nativeQuery = true)
    List<OverviewBySystemRow> fetchOverviewBySystem();

    // ========================================
    // SPRINT 5: Listagem com filtros avançados
    // ========================================

    /**
     * Interface estendida para listagem com campos de SLA e responsável.
     */
    interface IssueDetailRow {
        Long getId();
        String getIssueType();
        String getSeverity();
        String getStatus();
        Long getArticleId();
        String getArticleTitle();
        String getSystemCode();
        String getSystemName();
        String getMessage();
        java.time.Instant getCreatedAt();
        java.time.Instant getUpdatedAt();
        String getAssignedAgentId();
        String getAssignedAgentName();
        java.time.Instant getDueDate();
        // Sprint 5
        String getResponsibleId();
        String getResponsibleType();
        java.time.Instant getSlaDueAt();
        java.time.Instant getResolvedAt();
        String getIgnoredReason();
    }

    /**
     * Página de issues com filtros avançados (Sprint 5).
     *
     * <p>Filtros suportados:
     * <ul>
     *   <li>issueType, severity, status, systemCode - filtros básicos</li>
     *   <li>responsibleType, responsibleId - filtro de responsável direto</li>
     *   <li>overdue - filtra issues vencidas (sla_due_at &lt; now)</li>
     *   <li>unassigned - filtra issues sem responsável</li>
     * </ul>
     *
     * <p>Ordenação: vencidas primeiro (sla_due_at asc), depois por severidade desc.
     */
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
          i.created_at        AS createdAt,
          i.updated_at        AS updatedAt,
          last_assign.agent_id AS assignedAgentId,
          last_assign.agent_name AS assignedAgentName,
          last_assign.due_date AS dueDate,
          i.responsible_id    AS responsibleId,
          i.responsible_type  AS responsibleType,
          i.sla_due_at        AS slaDueAt,
          i.resolved_at       AS resolvedAt,
          i.ignored_reason    AS ignoredReason
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        LEFT JOIN LATERAL (
            SELECT ia.agent_id, ia.agent_name, ia.due_date
            FROM kb_governance_issue_assignment ia
            WHERE ia.issue_id = i.id
            ORDER BY ia.created_at DESC
            LIMIT 1
        ) last_assign ON true
        WHERE a.article_status = 1
          AND (:issueType IS NULL OR i.issue_type = :issueType)
          AND (:severity IS NULL OR i.severity = :severity)
          AND (:status IS NULL OR i.status = :status)
          AND (:systemCode IS NULL OR s.code = :systemCode)
          AND (:responsibleType IS NULL OR i.responsible_type = :responsibleType)
          AND (:responsibleId IS NULL OR i.responsible_id = :responsibleId)
          AND (:overdue IS NULL OR :overdue = FALSE OR (i.sla_due_at < NOW() AND i.status NOT IN ('RESOLVED', 'IGNORED')))
          AND (:unassigned IS NULL OR :unassigned = FALSE OR i.responsible_id IS NULL)
        ORDER BY
          CASE WHEN i.sla_due_at < NOW() AND i.status NOT IN ('RESOLVED', 'IGNORED') THEN 0 ELSE 1 END,
          i.sla_due_at ASC NULLS LAST,
          CASE i.severity WHEN 'ERROR' THEN 1 WHEN 'WARN' THEN 2 WHEN 'INFO' THEN 3 ELSE 4 END,
          i.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        WHERE a.article_status = 1
          AND (:issueType IS NULL OR i.issue_type = :issueType)
          AND (:severity IS NULL OR i.severity = :severity)
          AND (:status IS NULL OR i.status = :status)
          AND (:systemCode IS NULL OR s.code = :systemCode)
          AND (:responsibleType IS NULL OR i.responsible_type = :responsibleType)
          AND (:responsibleId IS NULL OR i.responsible_id = :responsibleId)
          AND (:overdue IS NULL OR :overdue = FALSE OR (i.sla_due_at < NOW() AND i.status NOT IN ('RESOLVED', 'IGNORED')))
          AND (:unassigned IS NULL OR :unassigned = FALSE OR i.responsible_id IS NULL)
        """,
            nativeQuery = true)
    Page<IssueDetailRow> pageIssuesAdvanced(
            Pageable pageable,
            @org.springframework.data.repository.query.Param("issueType") String issueType,
            @org.springframework.data.repository.query.Param("severity") String severity,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("systemCode") String systemCode,
            @org.springframework.data.repository.query.Param("responsibleType") String responsibleType,
            @org.springframework.data.repository.query.Param("responsibleId") String responsibleId,
            @org.springframework.data.repository.query.Param("overdue") Boolean overdue,
            @org.springframework.data.repository.query.Param("unassigned") Boolean unassigned
    );
}
