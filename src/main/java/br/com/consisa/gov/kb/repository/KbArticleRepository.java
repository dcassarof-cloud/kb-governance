package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repositório JPA para acesso à tabela kb_article.
 *
 * Responsabilidade:
 * - Consultas e persistência de KbArticle
 * - Sem regra de negócio (isso fica no Service)
 */
public interface KbArticleRepository extends JpaRepository<KbArticle, Long> {

    /**
     * Retorna no máximo 200 artigos que ainda não possuem sistema associado
     * (system_id IS NULL), ordenados pelos mais recentemente atualizados.
     */
    List<KbArticle> findTop200BySystemIsNullOrderByUpdatedDateDesc();

    /**
     * DELTA_WINDOW (produção):
     * Pega IDs que mudaram recentemente no Movidesk (updatedDate >= since),
     * ignorando itens "mortos" (NOT_FOUND / MISSING) pra não ficar dando 404 em loop.
     *
     * Observação:
     * - Não usa fetchedAt para evitar re-sync infinito (fetchedAt muda em todo sync).
     */
    @Query("""
        select a.id
        from KbArticle a
        where
            a.updatedDate is not null
            and a.updatedDate >= :since
            and (a.syncStatus is null or a.syncStatus <> 'NOT_FOUND')
            and (a.syncState is null or a.syncState <> 'MISSING')
        order by a.updatedDate desc
    """)
    List<Long> findIdsForDeltaSince(@Param("since") OffsetDateTime since);

    /**
     * (Opcional / legado) Se você ainda usa em algum lugar, mantenha.
     * Caso não use, pode remover.
     */
    @Query("""
        select a.id
        from KbArticle a
        where
            (a.updatedDate is not null and a.updatedDate >= :since)
            or (a.syncStatus is not null and a.syncStatus <> 'OK')
            or (a.system is null)
    """)
    List<Long> findDeltaIds(@Param("since") OffsetDateTime since);

    @Query("""
        select a.id
        from KbArticle a
        where a.system.code = 'GERAL'
    """)
    List<Long> findIdsInGeral();
}
