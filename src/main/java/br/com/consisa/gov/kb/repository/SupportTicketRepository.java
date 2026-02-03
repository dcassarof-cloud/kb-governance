package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Optional<SupportTicket> findByExternalTicketId(String externalTicketId);
}
