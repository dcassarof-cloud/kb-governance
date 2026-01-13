package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface KbArticleRepository extends JpaRepository<KbArticle, Long> {

    List<KbArticle> findTop200BySystemIsNullOrderByUpdatedDateDesc();

    /**
     * DELTA profissional:
     * - updatedDate > since
     * - ignora NOT_FOUND / MISSING
     */
    @Query("""
        select a.id
        from KbArticle a
        where
            a.updatedDate is not null
            and a.updatedDate > :since
            and (a.syncStatus is null or a.syncStatus <> 'NOT_FOUND')
            and (a.syncState is null or a.syncState <> 'MISSING')
        order by a.updatedDate desc
    """)
    List<Long> findDeltaIdsAfter(@Param("since") OffsetDateTime since);

    // ⚠️ NÃO usar isso no DELTA (governança separada)
    @Query("""
        select a.id
        from KbArticle a
        where a.system.code = 'GERAL'
    """)
    List<Long> findIdsInGeral();
}
