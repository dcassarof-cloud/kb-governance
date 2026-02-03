package br.com.consisa.gov.kb.controller.api.dto;

/**
 * Request para alterar status de uma issue (Sprint 5).
 *
 * <p>Status válidos: OPEN, IN_PROGRESS, RESOLVED, IGNORED
 *
 * <p>Regras:
 * <ul>
 *   <li>IGNORED requer ignoredReason preenchido</li>
 *   <li>Transição para RESOLVED define resolvedAt = now</li>
 *   <li>Transição de RESOLVED para OPEN recalcula SLA</li>
 * </ul>
 *
 * @param status        novo status
 * @param ignoredReason motivo (obrigatório se status = IGNORED)
 *
 * @author KB Governance Team
 * @since Sprint 5
 */
public record GovernanceIssueChangeStatusRequest(
        String status,
        String ignoredReason
) {

    /**
     * Valida se o request é válido.
     *
     * @return true se válido
     */
    public boolean isValid() {
        if (status == null || status.isBlank()) {
            return false;
        }

        // Se status é IGNORED, ignoredReason é obrigatório
        if ("IGNORED".equalsIgnoreCase(status)) {
            return ignoredReason != null && !ignoredReason.isBlank();
        }

        return true;
    }

    /**
     * Retorna o status normalizado (uppercase).
     *
     * @return status em uppercase
     */
    public String normalizedStatus() {
        return status != null ? status.toUpperCase() : null;
    }
}
