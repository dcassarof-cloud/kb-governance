package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.FaqClusterTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;

public interface FaqClusterTicketRepository extends JpaRepository<FaqClusterTicket, Long> {
    boolean existsByClusterIdAndTicketId(Long clusterId, Long ticketId);

    long countByClusterId(Long clusterId);

    @Query(value = """
        SELECT st.subject
        FROM faq_cluster_ticket ct
        JOIN support_ticket st ON st.id = ct.ticket_id
        WHERE ct.cluster_id = :clusterId
        ORDER BY st.origin_created_at DESC NULLS LAST, st.created_at DESC
        LIMIT 1
        """, nativeQuery = true)
    String findLatestSubjectByClusterId(Long clusterId);

    @Query(value = """
        SELECT COALESCE(MAX(st.origin_created_at), MAX(st.created_at))
        FROM faq_cluster_ticket ct
        JOIN support_ticket st ON st.id = ct.ticket_id
        WHERE ct.cluster_id = :clusterId
        """, nativeQuery = true)
    OffsetDateTime findLatestOccurrenceAtByClusterId(Long clusterId);

    @Query("""
        SELECT COUNT(ct)
        FROM FaqClusterTicket ct
        JOIN SupportTicket st ON st.id = ct.ticketId
        WHERE ct.clusterId = :clusterId
          AND st.originCreatedAt >= :cutoff
        """)
    long countTicketsInWindow(Long clusterId, OffsetDateTime cutoff);
}
