package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
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
     *
     * Por que limitar 200?
     * - Evita trazer milhares de registros de uma vez (performance/memória)
     * - Ajuda na curadoria manual via tela (paginação simples)
     *
     * Observação:
     * - Se no futuro precisar de paginação real, trocamos para Pageable.
     */
    List<KbArticle> findTop200BySystemIsNullOrderByUpdatedDateDesc();
    /**
     * Pega IDs para delta: artigos alterados recentemente (updatedDate) OU buscados recentemente (fetchedAt).
     * Ajuste conforme seu volume:
     * - se quiser focar só em "mudou no Movidesk": use apenas updatedDate
     * - se quiser manter fresco: fetchedAt também ajuda
     */
    @Query("""
        select a.id
        from KbArticle a
        where (a.updatedDate is not null and a.updatedDate >= :since)
           or (a.fetchedAt is not null and a.fetchedAt >= :since)
        order by coalesce(a.updatedDate, a.fetchedAt) desc
    """)
    List<Long> findIdsForDeltaSince(@Param("since") OffsetDateTime since);

}
