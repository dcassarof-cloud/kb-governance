package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbAgent;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 📊 Repository de Agentes da KB
 *
 * RESPONSABILIDADES:
 * ------------------
 * - CRUD de agentes
 * - Queries especializadas para seleção inteligente
 * - Busca por especialidade, equipe, produtividade
 *
 * ✅ VERSÃO CORRIGIDA - Todos os métodos implementados
 */
@Repository
public interface KbAgentRepository extends JpaRepository<KbAgent, String> {

    // ========================================
    // MÉTODOS DERIVADOS (Spring Data JPA)
    // ========================================

    /**
     * Busca todos agentes ativos
     */
    List<KbAgent> findByIsActiveTrue();

    /**
     * Busca todos agentes ativos ordenados por nome
     */
    List<KbAgent> findByIsActiveTrueOrderByBusinessNameAsc();

    /**
     * Conta total de agentes ativos
     */
    long countByIsActiveTrue();

    // ========================================
    // QUERIES JPQL CUSTOMIZADAS
    // ========================================

    /**
     * Busca agentes com especialidade específica ordenados por carga de trabalho
     *
     * Critérios:
     * 1. Tem especialidade no sistema
     * 2. Menor carga atual (assignedCount)
     * 3. Maior produtividade (completedCount)
     *
     * @param systemCode código do sistema (ex: CONSISANET, NOTAON)
     * @return lista de agentes ordenada por workload
     */
    @Query("""
        SELECT a FROM KbAgent a
        JOIN a.specialties s
        WHERE a.isActive = true
          AND s = :systemCode
        ORDER BY a.assignedCount ASC, a.completedCount DESC
    """)
    List<KbAgent> findBySpecialtyOrderByWorkload(@Param("systemCode") String systemCode);

    /**
     * Busca melhor agente para um sistema específico (retorna apenas 1)
     *
     * Mesma lógica do método acima, mas retorna Optional do primeiro resultado
     */
    @Query("""
        SELECT a FROM KbAgent a
        JOIN a.specialties s
        WHERE a.isActive = true
          AND s = :systemCode
        ORDER BY a.assignedCount ASC, a.completedCount DESC
    """)
    Optional<KbAgent> findBestForSystem(@Param("systemCode") String systemCode);

    /**
     * Busca agente menos ocupado (sem considerar especialidade)
     *
     * Fallback quando não há agente especialista disponível
     */
    @Query("""
        SELECT a FROM KbAgent a
        WHERE a.isActive = true
        ORDER BY a.assignedCount ASC, a.completedCount DESC
    """)
    Optional<KbAgent> findLeastBusy();

    /**
     * Top N agentes mais produtivos
     *
     * NOTA: Este método retorna List, então o service precisa limitar via PageRequest
     * ou manualmente com .stream().limit()
     *
     * @return lista ordenada por produtividade
     */
    @Query("""
        SELECT a FROM KbAgent a
        WHERE a.isActive = true
        ORDER BY a.completedCount DESC, a.assignedCount ASC
    """)
    List<KbAgent> findTopByProductivity(PageRequest pageRequest);

    /**
     * Busca agentes de uma equipe específica
     */
    @Query("""
        SELECT a FROM KbAgent a
        JOIN a.teams t
        WHERE a.isActive = true
          AND t = :teamName
        ORDER BY a.businessName ASC
    """)
    List<KbAgent> findByTeam(@Param("teamName") String teamName);

    /**
     * Busca agentes com especialidade específica
     */
    @Query("""
        SELECT a FROM KbAgent a
        JOIN a.specialties s
        WHERE a.isActive = true
          AND s = :specialty
        ORDER BY a.businessName ASC
    """)
    List<KbAgent> findBySpecialty(@Param("specialty") String specialty);
}