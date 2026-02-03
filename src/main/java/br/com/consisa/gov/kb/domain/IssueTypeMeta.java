package br.com.consisa.gov.kb.domain;

/**
 * Metadados de um tipo de issue de governança.
 *
 * <p>Usado para enriquecer a resposta da API com informações
 * amigáveis e recomendações de ação para cada tipo de issue.
 *
 * @param type           tipo da issue (enum)
 * @param displayName    nome amigável em pt-BR
 * @param description    descrição do problema
 * @param recommendation recomendação de ação
 *
 * @author KB Governance Team
 * @since Sprint 5
 */
public record IssueTypeMeta(
        KbGovernanceIssueType type,
        String displayName,
        String description,
        String recommendation
) {

    /**
     * Retorna o código do tipo como string.
     */
    public String typeCode() {
        return type != null ? type.name() : null;
    }
}
