package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidade que representa um sistema ou módulo interno
 * ao qual os artigos da Knowledge Base podem ser associados.
 *
 * Exemplos:
 * - QUINTOEIXO
 * - SGRH
 * - NOTAON
 *
 * Objetivo:
 * - Organizar e classificar artigos
 * - Facilitar curadoria e buscas
 * - Preparar a base para escalabilidade e IA
 */
@Entity
@Table(name = "kb_system")
public class KbSystem {

    /**
     * Identificador único do sistema (chave primária)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único do sistema.
     *
     * Usado como identificador lógico (ex: QUINTOEIXO).
     * Ideal para APIs, URLs e integrações.
     */
    @Column(nullable = false, unique = true, length = 60)
    private String code;

    /**
     * Nome amigável do sistema.
     */
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * Descrição detalhada do sistema ou módulo.
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * Indica se o sistema está ativo.
     *
     * Em vez de deletar registros, usamos
     * ativação/desativação (soft control).
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Data de criação do registro.
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Data da última atualização do registro.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Inicializa datas de criação e atualização
     * automaticamente ao inserir o registro.
     */
    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Atualiza automaticamente a data de modificação
     * sempre que o registro for alterado.
     */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /* ======================
       Getters e Setters
       ====================== */

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
