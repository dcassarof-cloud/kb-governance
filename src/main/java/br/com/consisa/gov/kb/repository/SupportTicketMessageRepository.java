package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.SupportTicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, Long> {
    Optional<SupportTicketMessage> findByExternalMessageKey(String externalMessageKey);
}
