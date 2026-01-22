package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
 */
@Repository
public interface KbAgentRepository extends JpaRepository<KbAgent, String> {

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

    /**
     * Busca agentes com especialidade específica ordenados por carga de trabalho
     * Critérios:
     * 1. Tem especialidade no sistema
     * 2. Menor carga atual (assignedCount)
     * 3. Maior produtividade (completedCount)
     */
    @Query("""
        SELECT a FROM KbAgent a
        WHERE a.isActive = true
        AND :systemCode MEMBER OF a.specialties
        ORDER BY a.assignedCount ASC, a.completedCount DESC
    """)
    List<KbAgent> findBySpecialtyOrderByWorkload(String systemCode);

    /**
     * Busca melhor agente para um sistema específico (retorna apenas 1)
     */
    @Query("""
        SELECT a FROM KbAgent a
        WHERE a.isActive = true
        AND :systemCode MEMBER OF a.specialties
        ORDER BY a.assignedCount ASC, a.completedCount DESC
    """)
    Optional<KbAgent> findBestForSystem(String systemCode);

    /**
     * Busca agente menos ocupado (sem considerar especialidade)
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
     */
    @Query("""
        SELECT a FROM KbAgent a
        WHERE a.isActive = true
        ORDER BY a.completedCount DESC, a.assignedCount ASC
    """)
    List<KbAgent> findTopByProductivity(int limit);

    /**
     * Busca agentes de uma equipe específica
     */
    @Query("""
        SELECT a FROM KbAgent a
        WHERE a.isActive = true
        AND :teamName MEMBER OF a.teams
        ORDER BY a.businessName ASC
    """)
    List<KbAgent> findByTeam(String teamName);

    /**
     * Busca agentes com especialidade específica
     */
    @Query("""
        SELECT a FROM KbAgent a
        WHERE a.isActive = true
        AND :specialty MEMBER OF a.specialties
        ORDER BY a.businessName ASC
    """)
    List<KbAgent> findBySpecialty(String specialty);
}