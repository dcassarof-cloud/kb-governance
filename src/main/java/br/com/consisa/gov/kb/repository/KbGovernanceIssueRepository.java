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
     * 📊 Conta artigos DISTINTOS com issues abertas (OPEN ou IN_PROGRESS).
     * Usado para calcular: OK = totalArtigos - artigosComIssueAberta
     *
     * REGRA DE NEGÓCIO (Sprint 1):
     * - "Issue aberta" = status OPEN ou IN_PROGRESS
     * - Quando analista assume issue (IN_PROGRESS), continua sendo problema aberto
     * - Um artigo com múltiplas issues abertas conta só uma vez
     * - Só deixa de contar quando TODAS as issues do artigo são RESOLVED
     */
    @Query("SELECT COUNT(DISTINCT i.articleId) FROM KbGovernanceIssue i " +
           "WHERE i.status IN (br.com.consisa.gov.kb.domain.GovernanceIssueStatus.OPEN, " +
           "br.com.consisa.gov.kb.domain.GovernanceIssueStatus.IN_PROGRESS)")
    long countDistinctArticlesWithOpenIssues();

    /**
     * 📊 Conta total de issues abertas (OPEN ou IN_PROGRESS).
     *
     * REGRA DE NEGÓCIO (Sprint 1):
     * - "Issue aberta" = OPEN ou IN_PROGRESS
     * - Issue em tratamento (IN_PROGRESS) ainda é problema aberto
     * - Só fecha quando status = RESOLVED
     */
    @Query("SELECT COUNT(i) FROM KbGovernanceIssue i " +
           "WHERE i.status IN (br.com.consisa.gov.kb.domain.GovernanceIssueStatus.OPEN, " +
           "br.com.consisa.gov.kb.domain.GovernanceIssueStatus.IN_PROGRESS)")
    long countOpenIssues();

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
          i.created_at        AS createdAt
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        LEFT JOIN kb_system s ON s.id = a.system_id
        WHERE a.article_status = 1
          AND (:issueType IS NULL OR i.issue_type = :issueType)
          AND (:status IS NULL OR i.status = :status)
        ORDER BY i.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM kb_governance_issue i
        JOIN kb_article a ON a.id = i.article_id
        WHERE a.article_status = 1
          AND (:issueType IS NULL OR i.issue_type = :issueType)
          AND (:status IS NULL OR i.status = :status)
        """,
            nativeQuery = true)
    Page<IssueRow> pageIssuesFiltered(
            Pageable pageable,
            @org.springframework.data.repository.query.Param("issueType") String issueType,
            @org.springframework.data.repository.query.Param("status") String status
    );
}
