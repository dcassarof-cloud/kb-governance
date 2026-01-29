package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.KbManualActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KbManualActionLogRepository extends JpaRepository<KbManualActionLog, Long> {
}
