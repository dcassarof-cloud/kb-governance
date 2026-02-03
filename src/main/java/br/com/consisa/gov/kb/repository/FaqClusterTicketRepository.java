package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.FaqClusterTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;

public interface FaqClusterTicketRepository extends JpaRepository<FaqClusterTicket, Long> {
    boolean existsByClusterIdAndTicketId(Long clusterId, Long ticketId);

    long countByClusterId(Long clusterId);

    @Query("""
        SELECT COUNT(ct)
        FROM FaqClusterTicket ct
        JOIN SupportTicket st ON st.id = ct.ticketId
        WHERE ct.clusterId = :clusterId
          AND st.originCreatedAt >= :cutoff
        """)
    long countTicketsInWindow(Long clusterId, OffsetDateTime cutoff);
}
