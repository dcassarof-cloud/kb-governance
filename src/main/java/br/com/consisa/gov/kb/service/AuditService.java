package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.AuditLog;
import br.com.consisa.gov.kb.repository.AuditLogRepository;
import br.com.consisa.gov.kb.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(String action, String entityType, String entityId, Object oldValue, Object newValue) {
        AuditLog logEntry = new AuditLog();
        Long userId = SecurityUtils.currentUserId();
        logEntry.setUserId(userId != null ? String.valueOf(userId) : null);
        logEntry.setAction(action);
        logEntry.setEntityType(entityType);
        logEntry.setEntityId(entityId);
        logEntry.setCorrelationId(MDC.get("correlationId"));
        logEntry.setOldValue(toJson(oldValue));
        logEntry.setNewValue(toJson(newValue));
        auditLogRepository.save(logEntry);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao serializar audit payload: {}", ex.getMessage());
            return null;
        }
    }
}
