package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para gerenciamento de agentes
 */
@Repository
public interface KbAgentRepository extends JpaRepository<KbAgent, String> {
    
    /**
     * Busca agente por username
     */
    Optional<KbAgent> findByUserName(String userName);
    
    /**
     * Lista todos os agentes ativos
     */
    List<KbAgent> findByIsActiveTrue();
    
    /**
     * Busca agentes com especialidade em um sistema específico,
     * ordenados por carga de trabalho (menor primeiro)
     */
    @Query("""
        SELECT DISTINCT a FROM KbAgent a
        JOIN a.specialties s
        WHERE a.isActive = TRUE
          AND s = :systemCode
        ORDER BY a.assignedCount ASC, a.completedCount DESC
        """)
    List<KbAgent> findBySpecialtyOrderByWorkload(@Param("systemCode") String systemCode);
    
    /**
     * Busca agentes de um time específico
     */
    @Query("""
        SELECT DISTINCT a FROM KbAgent a
        JOIN a.teams t
        WHERE a.isActive = TRUE
          AND t = :teamName
        ORDER BY a.assignedCount ASC
        """)
    List<KbAgent> findByTeamOrderByWorkload(@Param("teamName") String teamName);
    
    /**
     * Busca o agente com menor carga de trabalho (geral)
     */
    @Query("""
        SELECT a FROM KbAgent a
        WHERE a.isActive = TRUE
        ORDER BY a.assignedCount ASC, a.completedCount DESC
        LIMIT 1
        """)
    Optional<KbAgent> findLeastBusy();
    
    /**
     * Busca top N agentes mais produtivos
     */
    @Query("""
        SELECT a FROM KbAgent a
        WHERE a.isActive = TRUE
        ORDER BY a.completedCount DESC
        LIMIT :limit
        """)
    List<KbAgent> findTopByProductivity(@Param("limit") int limit);
    
    /**
     * Conta quantos agentes ativos existem
     */
    long countByIsActiveTrue();
}
