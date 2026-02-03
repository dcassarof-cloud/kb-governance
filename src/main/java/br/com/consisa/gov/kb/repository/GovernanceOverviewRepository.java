package br.com.consisa.gov.kb.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GovernanceOverviewRepository {

    private final EntityManager entityManager;

    public GovernanceOverviewRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public Object[] fetchOverviewTotals() {
        Query query = entityManager.createNativeQuery("""
            SELECT total_open, error_open, warn_open, info_open, overdue_open, unassigned_open
            FROM vw_kb_governance_overview
            """);
        List<Object[]> rows = query.getResultList();
        return rows.isEmpty() ? new Object[6] : rows.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> fetchOverviewBySystem() {
        Query query = entityManager.createNativeQuery("""
            SELECT system_code, system_name, total_open, error_open, warn_open, info_open, overdue_open, unassigned_open
            FROM vw_kb_governance_overview_by_system
            ORDER BY total_open DESC, system_code
            """);
        return query.getResultList();
    }
}
