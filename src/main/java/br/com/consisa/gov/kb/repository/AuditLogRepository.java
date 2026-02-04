package br.com.consisa.gov.kb.repository;

import br.com.consisa.gov.kb.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
