-- =====================================================
-- KB GOVERNANCE - MIGRATION V12
-- Sprint 5: SLA, Responsáveis Diretos, Overview Gerencial
-- =====================================================
-- Criado em: 2026-02-03
-- Autor: KB Governance Team
-- Descrição:
--   - Adiciona campos de SLA e responsável na issue
--   - Cria índices para consultas de overview
--   - Adiciona índice para histórico (já existe tabela)
-- =====================================================

-- ========================================
-- 1. ADICIONAR CAMPOS NA kb_governance_issue
-- ========================================

-- responsible_id: ID do responsável (usuário ou time)
ALTER TABLE kb_governance_issue
    ADD COLUMN IF NOT EXISTS responsible_id VARCHAR(100);

-- responsible_type: Tipo do responsável (USER ou TEAM)
ALTER TABLE kb_governance_issue
    ADD COLUMN IF NOT EXISTS responsible_type VARCHAR(10);

-- sla_due_at: Prazo de SLA calculado
ALTER TABLE kb_governance_issue
    ADD COLUMN IF NOT EXISTS sla_due_at TIMESTAMPTZ;

-- Comentários
COMMENT ON COLUMN kb_governance_issue.responsible_id IS 'ID do responsável atribuído (usuário ou time)';
COMMENT ON COLUMN kb_governance_issue.responsible_type IS 'Tipo do responsável: USER ou TEAM';
COMMENT ON COLUMN kb_governance_issue.sla_due_at IS 'Prazo de SLA calculado baseado na severidade (ERROR=3d, WARN=15d, INFO=30d)';

-- ========================================
-- 2. ÍNDICES PARA PERFORMANCE
-- ========================================

-- Índice para filtro de responsável
CREATE INDEX IF NOT EXISTS idx_kb_gov_issue_responsible
    ON kb_governance_issue(responsible_type, responsible_id);

-- Índice para filtro de SLA vencido
CREATE INDEX IF NOT EXISTS idx_kb_gov_issue_sla_due
    ON kb_governance_issue(sla_due_at)
    WHERE sla_due_at IS NOT NULL;

-- Índice para issues não atribuídas e abertas
CREATE INDEX IF NOT EXISTS idx_kb_gov_issue_unassigned_open
    ON kb_governance_issue(status)
    WHERE responsible_id IS NULL
      AND status NOT IN ('RESOLVED', 'IGNORED');

-- Índice composto para overview por sistema (via JOIN com artigo)
CREATE INDEX IF NOT EXISTS idx_kb_gov_issue_status_severity
    ON kb_governance_issue(status, severity);

-- ========================================
-- 3. ADICIONAR ÍNDICES NO HISTÓRICO
-- (tabela já existe desde V9)
-- ========================================

-- Índice para busca por action
CREATE INDEX IF NOT EXISTS idx_kb_gov_issue_history_action
    ON kb_governance_issue_history(action);

-- ========================================
-- 4. CALCULAR SLA INICIAL PARA ISSUES EXISTENTES
-- Regra:
--   ERROR  -> 3 dias
--   WARN   -> 15 dias
--   INFO   -> 30 dias
-- ========================================

UPDATE kb_governance_issue
SET sla_due_at = created_at + CASE
    WHEN severity = 'ERROR' THEN INTERVAL '3 days'
    WHEN severity = 'WARN'  THEN INTERVAL '15 days'
    WHEN severity = 'INFO'  THEN INTERVAL '30 days'
    ELSE INTERVAL '15 days'  -- default
END
WHERE sla_due_at IS NULL;

-- ========================================
-- 5. VIEW PARA OVERVIEW GERENCIAL
-- ========================================

CREATE OR REPLACE VIEW vw_kb_governance_overview AS
SELECT
    -- Totais gerais
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED')) AS open_count,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'ERROR') AS critical_open_count,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.responsible_id IS NULL) AS unassigned_count,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.sla_due_at < NOW()) AS overdue_count,
    COUNT(*) AS total_count
FROM kb_governance_issue i;

-- ========================================
-- 6. VIEW PARA OVERVIEW POR SISTEMA
-- ========================================

CREATE OR REPLACE VIEW vw_kb_governance_overview_by_system AS
SELECT
    COALESCE(s.code, 'UNCLASSIFIED') AS system_code,
    COALESCE(s.name, 'Não classificado') AS system_name,

    -- Contagens
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED')) AS open_count,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'ERROR') AS critical_count,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.sla_due_at < NOW()) AS overdue_count,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.responsible_id IS NULL) AS unassigned_count,

    -- Contagens por severidade (para cálculo de health score)
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'ERROR') AS error_count,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'WARN') AS warn_count,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'INFO') AS info_count

FROM kb_governance_issue i
JOIN kb_article a ON a.id = i.article_id
LEFT JOIN kb_system s ON s.id = a.system_id
WHERE a.article_status = 1  -- Apenas artigos ativos
GROUP BY s.code, s.name;

-- ========================================
-- VALIDAÇÃO
-- ========================================

DO $$
DECLARE
    issues_updated INTEGER;
    issues_with_sla INTEGER;
BEGIN
    SELECT COUNT(*) INTO issues_with_sla
    FROM kb_governance_issue
    WHERE sla_due_at IS NOT NULL;

    RAISE NOTICE '✅ Migration V12 concluída:';
    RAISE NOTICE '   - Issues com SLA calculado: %', issues_with_sla;
END $$;
