package br.com.consisa.gov.kb.domain;

/**
 * Motivos para atribuição de artigo
 */
public enum AssignmentReason {
    
    /**
     * Score de qualidade abaixo do mínimo aceitável (&lt;50)
     */
    QUALITY_LOW("Score de Qualidade Baixo"),
    
    /**
     * Artigo sem conteúdo ou com conteúdo muito reduzido
     */
    CONTENT_EMPTY("Conteúdo Vazio"),
    
    /**
     * Conteúdo desatualizado, precisa revisão
     */
    CONTENT_OUTDATED("Conteúdo Desatualizado"),
    
    /**
     * Artigo duplicado detectado
     */
    DUPLICATE("Artigo Duplicado"),
    
    /**
     * Artigo sem estrutura mínima (sem títulos, sem organização)
     */
    NO_STRUCTURE("Sem Estrutura Mínima"),
    
    /**
     * Solicitação manual de atualização
     */
    MANUAL_REQUEST("Solicitação Manual");
    
    private final String description;
    
    AssignmentReason(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
