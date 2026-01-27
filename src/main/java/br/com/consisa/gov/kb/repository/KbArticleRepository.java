package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * ✅ VERSÃO MELHORADA com queries adicionais para sync
 */
public interface KbArticleRepository extends JpaRepository<KbArticle, Long> {

    // =========================
    // ✅ QUERIES ORIGINAIS
    // =========================

    List<KbArticle> findTop200BySystemIsNullOrderByUpdatedDateDesc();

    @Query("""
        select a.id
        from KbArticle a
        where (a.updatedDate is not null and a.updatedDate >= :since)
           or (a.fetchedAt is not null and a.fetchedAt >= :since)
        order by coalesce(a.updatedDate, a.fetchedAt) desc
    """)
    List<Long> findIdsForDeltaSince(@Param("since") OffsetDateTime since);

    @Query("""
    select a
    from KbArticle a
    where a.contentHash is not null
      and a.contentHash <> ''
""")
    List<KbArticle> findAllWithContentHash();

    @Query("""
        select a.id
        from KbArticle a
        where
            a.updatedDate >= :since
            or a.syncStatus <> 'OK'
            or a.system is null
    """)
    List<Long> findDeltaIds(@Param("since") OffsetDateTime since);

    List<KbArticle> findByContentHash(String contentHash);

    @Query("""
        select a.id
        from KbArticle a
        where a.system.code = 'GERAL'
    """)
    List<Long> findIdsInGeral();

    @Query("""
            select a.contentHash
           from KbArticle a
           where a.contentHash is not null
             and a.contentHash <> ''
           group by a.contentHash
           having count(a.id) > 1
           order by count(a.id) desc
    """)
    List<String> findDuplicateHashes();

    List<KbArticle> findByContentHashOrderByUpdatedDateDesc(String contentHash);

    @Query("""
    select a.contentHash
    from KbArticle a
    where a.contentHash is not null and a.contentHash <> ''
    group by a.contentHash
    having count(a) > 1
""")
    List<String> findDuplicateContentHashes();

    @Query("""
    select a.id
    from KbArticle a
    where a.contentHash = :hash
    order by a.updatedDate desc nulls last
""")
    List<Long> findArticleIdsByContentHash(@Param("hash") String hash);

    @Query("""
        select a
        from KbArticle a
        order by coalesce(a.updatedDate, a.createdDate) desc
    """)
    Page<KbArticle> findRecent(Pageable pageable);

    // =========================
    // ✅ NOVAS QUERIES (V2)
    // =========================

    /**
     * 🗑️ Marca artigos como MISSING quando não foram vistos há muito tempo.
     *
     * Usado após FULL SYNC para detectar artigos deletados no Movidesk.
     *
     * @param cutoff data limite (ex: agora - 2 horas)
     * @return quantidade de artigos marcados
     */
    @Modifying
    @Query("""
        update KbArticle a
        set a.syncState = 'MISSING',
            a.syncStatus = 'NOT_FOUND'
        where a.lastSeenAt < :cutoff
          and a.syncState <> 'MISSING'
          and a.articleStatus = 1
    """)
    int markMissingArticles(@Param("cutoff") OffsetDateTime cutoff);

    /**
     * 🔍 Busca artigos que falharam no último sync.
     *
     * Útil para retry automático.
     *
     * @param limit máximo de artigos
     */
    @Query("""
        select a.id
        from KbArticle a
        where a.syncStatus = 'ERROR'
          and a.articleStatus = 1
        order by a.updatedDate desc
    """)
    List<Long> findFailedArticles(Pageable pageable);

    /**
     * 📊 Estatísticas de sync por status.
     */
    @Query("""
        select a.syncStatus, count(a)
        from KbArticle a
        where a.articleStatus = 1
        group by a.syncStatus
    """)
    List<Object[]> countBySyncStatus();

    /**
     * 📊 Estatísticas de sync por estado.
     */
    @Query("""
        select a.syncState, count(a)
        from KbArticle a
        where a.articleStatus = 1
        group by a.syncState
    """)
    List<Object[]> countBySyncState();

    /**
     * 🔄 Busca IDs de artigos para DELTA inteligente.
     *
     * Inclui:
     * - Artigos alterados desde :since
     * - Artigos com erro de sync
     * - Artigos sem classificação
     * - Artigos nunca sincronizados
     *
     * @param since data de corte
     * @param limit máximo de IDs
     */
    @Query("""
        select distinct a.id
        from KbArticle a
        where a.articleStatus = 1
          and (
              a.updatedDate >= :since
              or a.syncStatus <> 'OK'
              or a.system is null
              or a.lastSeenAt is null
          )
        order by coalesce(a.updatedDate, a.createdDate) desc
    """)
    List<Long> findDeltaSmartIds(@Param("since") OffsetDateTime since, Pageable pageable);

    /**
     * 📈 Conta artigos por sistema e status de sync.
     *
     * Útil para dashboard.
     */
    @Query("""
        select s.code, a.syncStatus, count(a)
        from KbArticle a
        left join a.system s
        where a.articleStatus = 1
        group by s.code, a.syncStatus
        order by s.code, a.syncStatus
    """)
    List<Object[]> countBySystemAndSyncStatus();

    /**
     * 🎯 Busca artigos prontos para análise de governança.
     *
     * Critérios:
     * - sync_status = OK
     * - conteúdo não vazio
     * - sistema classificado
     */
    @Query("""
        select a
        from KbArticle a
        where a.articleStatus = 1
          and a.syncStatus = 'OK'
          and a.contentHash is not null
          and a.system is not null
        order by a.updatedDate desc
    """)
    Page<KbArticle> findReadyForGovernance(Pageable pageable);

    /**
     * 🔄 Busca artigos para retry (falharam mas não há issue aberta).
     */
    @Query("""
        select a.id
        from KbArticle a
        where a.syncStatus = 'ERROR'
          and a.articleStatus = 1
          and not exists (
              select 1
              from KbSyncIssue i
              where i.articleId = a.id
                and i.resolved = false
          )
        order by a.updatedDate desc
    """)
    List<Long> findArticlesForRetry(Pageable pageable);


    @Query("select count(a) from KbArticle a where a.articleStatus = 1")
    long countActiveArticles();

    @Query("select count(a) from KbArticle a where a.articleStatus = 1 and a.governanceStatus = :status")
    long countActiveByGovernanceStatus(@Param("status") String status);

    /**
     * Contagem por sistema (inclui não classificados).
     * Retorna linhas: system_code, system_name, count
     */
    @Query(value = """
        SELECT
          COALESCE(s.code, 'UNCLASSIFIED') AS system_code,
          COALESCE(s.name, 'Não classificado') AS system_name,
          COUNT(*) AS cnt
        FROM kb_article a
        LEFT JOIN kb_system s ON s.id = a.system_id
        WHERE a.article_status = 1
        GROUP BY COALESCE(s.code, 'UNCLASSIFIED'), COALESCE(s.name, 'Não classificado')
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<Object[]> countActiveBySystem();

    /**
     * Contagem por governance_status (PENDING/APPROVED/REJECTED)
     * Retorna linhas: status, count
     */
    @Query(value = """
        SELECT a.governance_status AS status, COUNT(*) AS cnt
        FROM kb_article a
        WHERE a.article_status = 1
        GROUP BY a.governance_status
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<Object[]> countActiveByGovernanceStatus();

    /**
     * Grupos de duplicados (por hash)
     * Retorna: content_hash, count, array_agg(ids)
     */
    @Query(value = """
        SELECT
          a.content_hash AS hash,
          COUNT(*) AS cnt,
          ARRAY_AGG(a.id ORDER BY a.updated_date DESC NULLS LAST) AS ids
        FROM kb_article a
        WHERE a.article_status = 1
          AND a.content_hash IS NOT NULL
          AND a.content_hash <> ''
        GROUP BY a.content_hash
        HAVING COUNT(*) > 1
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<Object[]> findDuplicateGroups();

}