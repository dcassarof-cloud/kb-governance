package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbGovernanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository para snapshots históricos de governança
 */
@Repository
public interface KbGovernanceSnapshotRepository extends JpaRepository<KbGovernanceSnapshot, Long> {

    // ======================
    // CONSULTAS BÁSICAS
    // ======================

    /**
     * Busca snapshot global de uma data específica
     */
    Optional<KbGovernanceSnapshot> findBySnapshotDateAndSystemCodeIsNull(LocalDate date);

    /**
     * Busca snapshot de um sistema em uma data específica
     */
    Optional<KbGovernanceSnapshot> findBySnapshotDateAndSystemCode(LocalDate date, String systemCode);

    /**
     * Busca todos os snapshots de uma data
     */
    List<KbGovernanceSnapshot> findBySnapshotDateOrderBySystemCode(LocalDate date);

    // ======================
    // TENDÊNCIAS
    // ======================

    /**
     * Busca snapshots globais de um período
     */
    @Query("""
        SELECT s FROM KbGovernanceSnapshot s
        WHERE s.snapshotDate BETWEEN :startDate AND :endDate
          AND s.systemCode IS NULL
        ORDER BY s.snapshotDate DESC
    """)
    List<KbGovernanceSnapshot> findGlobalSnapshotsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Busca snapshots de um sistema em um período
     */
    @Query("""
        SELECT s FROM KbGovernanceSnapshot s
        WHERE s.snapshotDate BETWEEN :startDate AND :endDate
          AND s.systemCode = :systemCode
        ORDER BY s.snapshotDate DESC
    """)
    List<KbGovernanceSnapshot> findSystemSnapshotsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("systemCode") String systemCode
    );

    /**
     * Busca últimos N snapshots globais
     */
    @Query("""
        SELECT s FROM KbGovernanceSnapshot s
        WHERE s.systemCode IS NULL
        ORDER BY s.snapshotDate DESC
        LIMIT :limit
    """)
    List<KbGovernanceSnapshot> findLatestGlobalSnapshots(@Param("limit") int limit);

    /**
     * Busca últimos N snapshots de um sistema
     */
    @Query("""
        SELECT s FROM KbGovernanceSnapshot s
        WHERE s.systemCode = :systemCode
        ORDER BY s.snapshotDate DESC
        LIMIT :limit
    """)
    List<KbGovernanceSnapshot> findLatestSystemSnapshots(
            @Param("systemCode") String systemCode,
            @Param("limit") int limit
    );

    // ======================
    // COMPARAÇÕES
    // ======================

    /**
     * Busca snapshot mais recente (global)
     */
    @Query("""
        SELECT s FROM KbGovernanceSnapshot s
        WHERE s.systemCode IS NULL
        ORDER BY s.snapshotDate DESC
        LIMIT 1
    """)
    Optional<KbGovernanceSnapshot> findLatestGlobal();

    /**
     * Busca snapshot mais recente de um sistema
     */
    @Query("""
        SELECT s FROM KbGovernanceSnapshot s
        WHERE s.systemCode = :systemCode
        ORDER BY s.snapshotDate DESC
        LIMIT 1
    """)
    Optional<KbGovernanceSnapshot> findLatestForSystem(@Param("systemCode") String systemCode);

    // ======================
    // LIMPEZA
    // ======================

    /**
     * Deleta snapshots antigos (para manutenção)
     */
    void deleteBySnapshotDateBefore(LocalDate date);

    /**
     * Verifica se já existe snapshot
     */
    boolean existsBySnapshotDateAndSystemCode(LocalDate date, String systemCode);

    // ======================
    // ESTATÍSTICAS
    // ======================

    /**
     * Conta total de snapshots globais
     */
    @Query("SELECT COUNT(s) FROM KbGovernanceSnapshot s WHERE s.systemCode IS NULL")
    long countGlobalSnapshots();

    /**
     * Conta snapshots de um sistema
     */
    long countBySystemCode(String systemCode);
}
