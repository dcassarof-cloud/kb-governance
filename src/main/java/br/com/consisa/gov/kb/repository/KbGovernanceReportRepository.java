package br.com.consisa.gov.kb.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para consultar o relatório de governança
 *
 * ✅ CORRIGIDO: Usa EntityManager diretamente (não estende JpaRepository)
 * porque estamos consultando uma VIEW, não uma entidade gerenciada.
 */
@Repository
public class KbGovernanceReportRepository {

    private final EntityManager entityManager;

    public KbGovernanceReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Busca relatório completo (todos os artigos)
     */
    public List<Object[]> findAllReport() {
        String sql = """
            select
                article_id,
                system_code,
                system_name,
                title,
                content_hash,
                source_url,
                updated_date,
                is_empty,
                is_too_short,
                is_duplicate_same_system,
                is_hash_reused_other_system,
                lacks_min_structure,
                content_length,
                header_count,
                has_lists,
                has_action_verbs,
                has_system_context
            from kb_article_governance_report
            order by 
                (case when is_empty then 1 else 0 end +
                 case when is_duplicate_same_system then 1 else 0 end +
                 case when lacks_min_structure then 1 else 0 end) desc,
                system_code,
                title
        """;

        Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }

    /**
     * Busca relatório filtrado por sistema
     */
    public List<Object[]> findBySystemCode(String systemCode) {
        String sql = """
            select
                article_id,
                system_code,
                system_name,
                title,
                content_hash,
                source_url,
                updated_date,
                is_empty,
                is_too_short,
                is_duplicate_same_system,
                is_hash_reused_other_system,
                lacks_min_structure,
                content_length,
                header_count,
                has_lists,
                has_action_verbs,
                has_system_context
            from kb_article_governance_report
            where system_code = :systemCode
            order by 
                (case when is_empty then 1 else 0 end +
                 case when is_duplicate_same_system then 1 else 0 end +
                 case when lacks_min_structure then 1 else 0 end) desc,
                title
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("systemCode", systemCode);
        return query.getResultList();
    }

    /**
     * Busca apenas artigos com problemas
     */
    public List<Object[]> findWithIssuesOnly() {
        String sql = """
            select
                article_id,
                system_code,
                system_name,
                title,
                content_hash,
                source_url,
                updated_date,
                is_empty,
                is_too_short,
                is_duplicate_same_system,
                is_hash_reused_other_system,
                lacks_min_structure,
                content_length,
                header_count,
                has_lists,
                has_action_verbs,
                has_system_context
            from kb_article_governance_report
            where is_empty = true
               or is_too_short = true
               or is_duplicate_same_system = true
               or lacks_min_structure = true
            order by 
                (case when is_empty then 1 else 0 end +
                 case when is_duplicate_same_system then 1 else 0 end +
                 case when lacks_min_structure then 1 else 0 end) desc,
                system_code,
                title
        """;

        Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }

    /**
     * Busca artigos IA-Ready (sem problemas críticos)
     */
    public List<Object[]> findIaReady() {
        String sql = """
            select
                article_id,
                system_code,
                system_name,
                title,
                content_hash,
                source_url,
                updated_date,
                is_empty,
                is_too_short,
                is_duplicate_same_system,
                is_hash_reused_other_system,
                lacks_min_structure,
                content_length,
                header_count,
                has_lists,
                has_action_verbs,
                has_system_context
            from kb_article_governance_report
            where is_empty = false
              and is_duplicate_same_system = false
              and lacks_min_structure = false
            order by system_code, title
        """;

        Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }

    /**
     * Conta total de artigos por categoria
     */
    public Object[] getSummaryStats() {
        String sql = """
            select
                count(*) as total,
                sum(case when is_empty then 1 else 0 end) as empty_count,
                sum(case when is_too_short then 1 else 0 end) as short_count,
                sum(case when is_duplicate_same_system then 1 else 0 end) as duplicate_count,
                sum(case when is_hash_reused_other_system then 1 else 0 end) as hash_reused_count,
                sum(case when lacks_min_structure then 1 else 0 end) as no_structure_count,
                sum(case when is_empty = false 
                          and is_duplicate_same_system = false 
                          and lacks_min_structure = false 
                     then 1 else 0 end) as ia_ready_count
            from kb_article_governance_report
        """;

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();

        return results.isEmpty() ? new Object[7] : results.get(0);
    }
}