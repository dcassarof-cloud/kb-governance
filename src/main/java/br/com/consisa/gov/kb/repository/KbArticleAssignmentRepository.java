package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.AssignmentStatus;
import br.com.consisa.gov.kb.domain.KbAgent;
import br.com.consisa.gov.kb.domain.KbArticleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gerenciamento de atribuições
 */
@Repository
public interface KbArticleAssignmentRepository extends JpaRepository<KbArticleAssignment, Long> {

    /**
     * Busca a atribuição ativa mais recente de um artigo
     */
    @Query("""
        SELECT a FROM KbArticleAssignment a
        WHERE a.articleId = :articleId
          AND a.status IN ('PENDING', 'IN_PROGRESS')
        ORDER BY a.createdAt DESC
        LIMIT 1
        """)
    Optional<KbArticleAssignment> findActiveByArticleId(@Param("articleId") Long articleId);

    /**
     * Lista atribuições de um agente por status
     */
    List<KbArticleAssignment> findByAgentAndStatusOrderByCreatedAtDesc(
            KbAgent agent,
            AssignmentStatus status
    );

    /**
     * Lista todas atribuições de um agente
     */
    List<KbArticleAssignment> findByAgentOrderByCreatedAtDesc(KbAgent agent);

    /**
     * Lista atribuições atrasadas (status ativo e prazo vencido)
     */
    @Query("""
        SELECT a FROM KbArticleAssignment a
        WHERE a.status IN ('PENDING', 'IN_PROGRESS')
          AND a.dueDate IS NOT NULL
          AND a.dueDate < :now
        ORDER BY a.dueDate ASC
        """)
    List<KbArticleAssignment> findOverdue(@Param("now") OffsetDateTime now);

    /**
     * Conta atribuições ativas de um agente
     */
    @Query("""
        SELECT COUNT(a) FROM KbArticleAssignment a
        WHERE a.agent = :agent
          AND a.status IN ('PENDING', 'IN_PROGRESS')
        """)
    long countActiveByAgent(@Param("agent") KbAgent agent);

    /**
     * Lista atribuições por status
     */
    List<KbArticleAssignment> findByStatusOrderByCreatedAtDesc(AssignmentStatus status);

    /**
     * Busca estatísticas agregadas
     *
     * ✅ CORRIGIDO: Retorna List<Object[]> ao invés de Object[]
     *
     * JPQL queries sempre retornam List, mesmo quando há apenas um resultado.
     * O service deve chamar .get(0) para pegar o primeiro elemento.
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN a.status = 'PENDING' THEN 1 ELSE 0 END) as pending,
            SUM(CASE WHEN a.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgress,
            SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN a.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled
        FROM KbArticleAssignment a
        """)
    List<Object[]> getStatistics();

    /**
     * Lista atribuições criadas após uma data
     */
    List<KbArticleAssignment> findByCreatedAtAfterOrderByCreatedAtDesc(OffsetDateTime date);

    /**
     * Verifica se um artigo já tem atribuição ativa
     */
    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
        FROM KbArticleAssignment a
        WHERE a.articleId = :articleId
          AND a.status IN ('PENDING', 'IN_PROGRESS')
        """)
    boolean existsActiveByArticleId(@Param("articleId") Long articleId);
}