package br.com.consisa.gov.kb.domain;

/**
 * Prioridades para atribuições
 */
public enum AssignmentPriority {
    
    /**
     * Prioridade baixa - pode aguardar
     */
    LOW("Baixa", 1),
    
    /**
     * Prioridade média - prazo normal
     */
    MEDIUM("Média", 2),
    
    /**
     * Prioridade alta - prazo curto
     */
    HIGH("Alta", 3),
    
    /**
     * Urgente - precisa atenção imediata
     */
    URGENT("Urgente", 4);
    
    private final String description;
    private final int level;
    
    AssignmentPriority(String description, int level) {
        this.description = description;
        this.level = level;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * Determina prioridade baseada no score de qualidade
     */
    public static AssignmentPriority fromQualityScore(int score) {
        if (score < 20) return URGENT;
        if (score < 35) return HIGH;
        if (score < 50) return MEDIUM;
        return LOW;
    }
}
