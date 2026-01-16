package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.dto.KbArticleGovernanceReportDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository para consultar o relatório de governança
 *
 * Lê diretamente da VIEW kb_article_governance_report
 */
public interface KbGovernanceReportRepository extends Repository<Object, Long> {

    /**
     * Busca relatório completo (todos os artigos)
     */
    @Query(value = """
        select
            article_id as articleId,
            system_code as systemCode,
            system_name as systemName,
            title,
            content_hash as contentHash,
            source_url as sourceUrl,
            updated_date as updatedDate,
            is_empty as isEmpty,
            is_too_short as isTooShort,
            is_duplicate_same_system as isDuplicateSameSystem,
            is_hash_reused_other_system as isHashReusedOtherSystem,
            lacks_min_structure as lacksMinStructure,
            content_length as contentLength,
            header_count as headerCount,
            has_lists as hasLists,
            has_action_verbs as hasActionVerbs,
            has_system_context as hasSystemContext
        from kb_article_governance_report
        order by 
            (case when is_empty then 1 else 0 end +
             case when is_duplicate_same_system then 1 else 0 end +
             case when lacks_min_structure then 1 else 0 end) desc,
            system_code,
            title
    """, nativeQuery = true)
    List<Object[]> findAllReport();

    /**
     * Busca relatório filtrado por sistema
     */
    @Query(value = """
        select
            article_id as articleId,
            system_code as systemCode,
            system_name as systemName,
            title,
            content_hash as contentHash,
            source_url as sourceUrl,
            updated_date as updatedDate,
            is_empty as isEmpty,
            is_too_short as isTooShort,
            is_duplicate_same_system as isDuplicateSameSystem,
            is_hash_reused_other_system as isHashReusedOtherSystem,
            lacks_min_structure as lacksMinStructure,
            content_length as contentLength,
            header_count as headerCount,
            has_lists as hasLists,
            has_action_verbs as hasActionVerbs,
            has_system_context as hasSystemContext
        from kb_article_governance_report
        where system_code = :systemCode
        order by 
            (case when is_empty then 1 else 0 end +
             case when is_duplicate_same_system then 1 else 0 end +
             case when lacks_min_structure then 1 else 0 end) desc,
            title
    """, nativeQuery = true)
    List<Object[]> findBySystemCode(@Param("systemCode") String systemCode);

    /**
     * Busca apenas artigos com problemas
     */
    @Query(value = """
        select
            article_id as articleId,
            system_code as systemCode,
            system_name as systemName,
            title,
            content_hash as contentHash,
            source_url as sourceUrl,
            updated_date as updatedDate,
            is_empty as isEmpty,
            is_too_short as isTooShort,
            is_duplicate_same_system as isDuplicateSameSystem,
            is_hash_reused_other_system as isHashReusedOtherSystem,
            lacks_min_structure as lacksMinStructure,
            content_length as contentLength,
            header_count as headerCount,
            has_lists as hasLists,
            has_action_verbs as hasActionVerbs,
            has_system_context as hasSystemContext
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
    """, nativeQuery = true)
    List<Object[]> findWithIssuesOnly();

    /**
     * Busca artigos IA-Ready (sem problemas críticos)
     */
    @Query(value = """
        select
            article_id as articleId,
            system_code as systemCode,
            system_name as systemName,
            title,
            content_hash as contentHash,
            source_url as sourceUrl,
            updated_date as updatedDate,
            is_empty as isEmpty,
            is_too_short as isTooShort,
            is_duplicate_same_system as isDuplicateSameSystem,
            is_hash_reused_other_system as isHashReusedOtherSystem,
            lacks_min_structure as lacksMinStructure,
            content_length as contentLength,
            header_count as headerCount,
            has_lists as hasLists,
            has_action_verbs as hasActionVerbs,
            has_system_context as hasSystemContext
        from kb_article_governance_report
        where is_empty = false
          and is_duplicate_same_system = false
          and lacks_min_structure = false
        order by system_code, title
    """, nativeQuery = true)
    List<Object[]> findIaReady();

    /**
     * Conta total de artigos por categoria
     */
    @Query(value = """
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
    """, nativeQuery = true)
    Object[] getSummaryStats();
}