package br.com.consisa.gov.kb.controller.api.dto;

/**
 * Request para atribuir responsável a uma issue (Sprint 5).
 *
 * <p>Diferente do endpoint POST /assign legado, este endpoint PUT
 * atribui diretamente na issue sem criar assignment separado.
 *
 * @param responsibleType tipo: USER ou TEAM
 * @param responsibleId   ID do responsável
 *
 * @author KB Governance Team
 * @since Sprint 5
 */
public record GovernanceIssueAssignResponsibleRequest(
        String responsibleType,
        String responsibleId
) {

    /**
     * Valida se o request é válido.
     *
     * @return true se válido
     */
    public boolean isValid() {
        // Para atribuir, ambos devem estar preenchidos
        // Para desatribuir, ambos podem ser null/blank
        if (responsibleId == null || responsibleId.isBlank()) {
            return responsibleType == null || responsibleType.isBlank();
        }
        return responsibleType != null && !responsibleType.isBlank();
    }

    /**
     * Verifica se é uma desatribuição (remoção de responsável).
     *
     * @return true se for desatribuição
     */
    public boolean isUnassign() {
        return (responsibleId == null || responsibleId.isBlank())
                && (responsibleType == null || responsibleType.isBlank());
    }
}
