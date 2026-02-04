package br.com.consisa.gov.kb.audit;

import br.com.consisa.gov.kb.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Optional;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @AfterReturning(
            pointcut = "@annotation(org.springframework.web.bind.annotation.PostMapping) " +
                    "|| @annotation(org.springframework.web.bind.annotation.PutMapping) " +
                    "|| @annotation(org.springframework.web.bind.annotation.PatchMapping) " +
                    "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)",
            returning = "result"
    )
    public void afterMutation(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = resolveRequest();
        if (request == null) {
            return;
        }

        String action = request.getMethod() + " " + request.getRequestURI();
        Object body = unwrapBody(result);
        String entityType = body != null ? body.getClass().getSimpleName() : joinPoint.getSignature().getDeclaringTypeName();
        String entityId = resolveEntityId(body, request.getRequestURI());

        try {
            auditService.record(action, entityType, entityId, null, body);
        } catch (Exception ex) {
            log.warn("Falha ao registrar auditoria: {}", ex.getMessage());
        }
    }

    private HttpServletRequest resolveRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private Object unwrapBody(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getBody();
        }
        return result;
    }

    private String resolveEntityId(Object body, String path) {
        if (body != null) {
            try {
                Method method = body.getClass().getMethod("getId");
                Object id = method.invoke(body);
                if (id != null) {
                    return String.valueOf(id);
                }
            } catch (Exception ignored) {
                // ignore
            }
        }

        String[] parts = Optional.ofNullable(path).orElse("").split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }
}
