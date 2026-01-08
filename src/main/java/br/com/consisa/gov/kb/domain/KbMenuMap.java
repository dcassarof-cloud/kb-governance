package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 🔗 Mapeamento oficial de Menu do Movidesk -> Sistema interno (KbSystem)
 *
 * 🎯 OBJETIVO DESTE ENTITY
 * -------------------------------------------------
 * - Eliminar lógica de if/else no código
 * - Centralizar decisão de classificação no BANCO
 * - Permitir ajustes de mapeamento SEM DEPLOY
 *
 * 🔐 GOVERNANÇA
 * -------------------------------------------------
 * - Um menu do Movidesk aponta para exatamente um sistema interno
 * - Apenas UM mapeamento ativo por (source_system + source_menu_id)
 * - Histórico preservado via is_active = false
 *
 * 📦 TABELA: kb_menu_map
 *
 * Colunas reais no banco:
 * - id
 * - source_system
 * - source_menu_id
 * - source_menu_name
 * - system_id (FK -> kb_system)
 * - is_active
 * - created_at
 * - updated_at
 */
@Entity
@Table(name = "kb_menu_map")
public class KbMenuMap {

    /**
     * ID técnico do mapeamento
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Fonte do menu.
     * Exemplo real:
     * - "movidesk"
     *
     * Permite no futuro:
     * - integrar outras KBs (Zendesk, Freshdesk etc.)
     */
    @Column(name = "source_system", nullable = false, length = 30)
    private String sourceSystem;

    /**
     * ID do menu na fonte externa (Movidesk).
     * ⚠️ ESSE É O IDENTIFICADOR OFICIAL
     */
    @Column(name = "source_menu_id", nullable = false)
    private Long sourceMenuId;

    /**
     * Nome do menu no Movidesk.
     *
     * ❗ Usado APENAS para:
     * - auditoria
     * - debug
     * - relatórios
     *
     * ❌ Nunca para lógica de negócio
     */
    @Column(name = "source_menu_name", nullable = false, length = 255)
    private String sourceMenuName;

    /**
     * Sistema interno associado a este menu.
     *
     * FetchType.LAZY:
     * - evita custo desnecessário em telas/listagens
     * - carregamos explicitamente quando necessário via EntityGraph
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false)
    private KbSystem system;

    /**
     * Flag de ativação do mapeamento.
     *
     * true  = mapeamento válido
     * false = histórico / desativado
     */
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    /**
     * Datas controladas pelo banco/Flyway (timestamptz)
     */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /* ======================
       GETTERS / SETTERS
       ====================== */

    public Long getId() { return id; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public Long getSourceMenuId() { return sourceMenuId; }
    public void setSourceMenuId(Long sourceMenuId) { this.sourceMenuId = sourceMenuId; }

    public String getSourceMenuName() { return sourceMenuName; }
    public void setSourceMenuName(String sourceMenuName) { this.sourceMenuName = sourceMenuName; }

    public KbSystem getSystem() { return system; }
    public void setSystem(KbSystem system) { this.system = system; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
