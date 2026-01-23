package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbArticleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para histórico de versões de artigos
 */
@Repository
public interface KbArticleVersionRepository extends JpaRepository<KbArticleVersion, Long> {

    /**
     * Busca todas as versões de um artigo (ordenadas da mais recente para a mais antiga)
     */
    List<KbArticleVersion> findByArticleIdOrderByVersionNumberDesc(Long articleId);

    /**
     * Busca versão específica de um artigo
     */
    Optional<KbArticleVersion> findByArticleIdAndVersionNumber(Long articleId, Integer versionNumber);

    /**
     * Busca última versão de um artigo
     */
    @Query("""
        SELECT v FROM KbArticleVersion v
        WHERE v.articleId = :articleId
        ORDER BY v.versionNumber DESC
        LIMIT 1
    """)
    Optional<KbArticleVersion> findLatestVersion(@Param("articleId") Long articleId);

    /**
     * Retorna maior número de versão de um artigo (para incremento)
     */
    @Query("""
        SELECT MAX(v.versionNumber)
        FROM KbArticleVersion v
        WHERE v.articleId = :articleId
    """)
    Optional<Integer> getMaxVersionNumber(@Param("articleId") Long articleId);

    /**
     * Conta quantas versões um artigo tem
     */
    long countByArticleId(Long articleId);

    /**
     * Busca versões por tipo de mudança
     */
    List<KbArticleVersion> findByArticleIdAndChangeType(Long articleId, String changeType);

    /**
     * Busca versões criadas por um usuário
     */
    @Query("""
        SELECT v FROM KbArticleVersion v
        WHERE v.articleId = :articleId
          AND v.changedBy = :changedBy
        ORDER BY v.versionNumber DESC
    """)
    List<KbArticleVersion> findByArticleAndUser(
            @Param("articleId") Long articleId,
            @Param("changedBy") String changedBy
    );

    /**
     * Deleta versões antigas (para limpeza/manutenção)
     * CUIDADO: Só usar em casos específicos, pois perde histórico!
     */
    void deleteByArticleIdAndVersionNumberLessThan(Long articleId, Integer versionNumber);
}
