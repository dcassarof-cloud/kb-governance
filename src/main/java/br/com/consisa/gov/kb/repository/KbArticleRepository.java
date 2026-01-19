package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repositório JPA para acesso à tabela kb_article.
 */
public interface KbArticleRepository extends JpaRepository<KbArticle, Long> {

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

    // =========================
    // ✅ DUPLICADOS (por hash)
    // =========================

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

    // =========================
    // ✅ RECENTES (para análise)
    // =========================
    @Query("""
        select a
        from KbArticle a
        order by coalesce(a.updatedDate, a.createdDate) desc
    """)
    Page<KbArticle> findRecent(Pageable pageable);
}
