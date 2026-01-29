package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KbNotificationRepository extends JpaRepository<KbNotification, Long> {
}
