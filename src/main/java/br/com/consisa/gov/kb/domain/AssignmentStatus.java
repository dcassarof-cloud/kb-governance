package br.com.consisa.gov.kb.domain;

/**
 * Status de uma atribuição
 */
public enum AssignmentStatus {
    
    /**
     * Pendente - aguardando início
     */
    PENDING("Pendente"),
    
    /**
     * Em andamento - agente está trabalhando
     */
    IN_PROGRESS("Em Andamento"),
    
    /**
     * Concluída - atualização finalizada
     */
    COMPLETED("Concluída"),
    
    /**
     * Cancelada - não será executada
     */
    CANCELLED("Cancelada");
    
    private final String description;
    
    AssignmentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Verifica se o status é ativo (pendente ou em andamento)
     */
    public boolean isActive() {
        return this == PENDING || this == IN_PROGRESS;
    }
    
    /**
     * Verifica se o status é final (concluído ou cancelado)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
