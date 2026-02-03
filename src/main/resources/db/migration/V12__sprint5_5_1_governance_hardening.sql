-- =====================================================
-- KB GOVERNANCE - MIGRATION V12
-- Sprint 5 + 5.1: SLA, responsáveis, histórico rico, hardening
-- =====================================================

-- ========================================
-- 1. Campos novos em kb_governance_issue
-- ========================================
ALTER TABLE kb_governance_issue
    ADD COLUMN IF NOT EXISTS responsible_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS responsible_type VARCHAR(10),
    ADD COLUMN IF NOT EXISTS sla_due_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS ignored_reason TEXT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE kb_governance_issue
SET updated_at = COALESCE(updated_at, created_at, NOW())
WHERE updated_at IS NULL;

-- ========================================
-- 2. Backfill de SLA por severidade (valores reais do domínio)
-- ========================================
UPDATE kb_governance_issue
SET sla_due_at = created_at + CASE severity
    WHEN 'ERROR' THEN INTERVAL '3 days'
    WHEN 'WARN' THEN INTERVAL '15 days'
    WHEN 'INFO' THEN INTERVAL '30 days'
    ELSE INTERVAL '15 days'
END
WHERE sla_due_at IS NULL;

-- ========================================
-- 3. Hardening de constraints
-- ========================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_kb_gov_issue_responsible_type'
    ) THEN
        ALTER TABLE kb_governance_issue
            ADD CONSTRAINT ck_kb_gov_issue_responsible_type
            CHECK (responsible_type IN ('USER', 'TEAM') OR responsible_type IS NULL);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_kb_gov_issue_status'
    ) THEN
        ALTER TABLE kb_governance_issue
            ADD CONSTRAINT ck_kb_gov_issue_status
            CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'IGNORED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_kb_gov_issue_severity'
    ) THEN
        ALTER TABLE kb_governance_issue
            ADD CONSTRAINT ck_kb_gov_issue_severity
            CHECK (severity IN ('INFO', 'WARN', 'ERROR'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_kb_gov_issue_assigned_responsible'
    ) THEN
        ALTER TABLE kb_governance_issue
            ADD CONSTRAINT ck_kb_gov_issue_assigned_responsible
            CHECK (status <> 'ASSIGNED' OR responsible_id IS NOT NULL);
    END IF;
END $$;

-- ========================================
-- 4. Índices adicionais
-- ========================================
CREATE INDEX IF NOT EXISTS idx_kb_gov_issue_responsible
    ON kb_governance_issue(responsible_type, responsible_id);

CREATE INDEX IF NOT EXISTS idx_kb_gov_issue_sla_due
    ON kb_governance_issue(sla_due_at);

CREATE INDEX IF NOT EXISTS idx_kb_gov_issue_unassigned_open
    ON kb_governance_issue(status, responsible_id);

CREATE INDEX IF NOT EXISTS idx_kb_gov_issue_status_severity
    ON kb_governance_issue(status, severity);

-- ========================================
-- 5. Histórico de mudanças (rich history)
-- ========================================
ALTER TABLE kb_governance_issue_history
    ALTER COLUMN old_value TYPE TEXT,
    ALTER COLUMN new_value TYPE TEXT,
    ALTER COLUMN actor SET DEFAULT 'system';

UPDATE kb_governance_issue_history
SET actor = 'system'
WHERE actor IS NULL;

CREATE INDEX IF NOT EXISTS idx_history_issue_id
    ON kb_governance_issue_history(issue_id);

CREATE INDEX IF NOT EXISTS idx_history_created_at
    ON kb_governance_issue_history(created_at);

-- ========================================
-- 6. Views de overview (ativos + overdue)
-- ========================================
CREATE OR REPLACE VIEW vw_kb_governance_overview AS
SELECT
    COUNT(*) FILTER (WHERE status NOT IN ('RESOLVED', 'IGNORED')) AS total_open,
    COUNT(*) FILTER (WHERE status NOT IN ('RESOLVED', 'IGNORED') AND severity = 'ERROR') AS error_open,
    COUNT(*) FILTER (WHERE status NOT IN ('RESOLVED', 'IGNORED') AND severity = 'WARN') AS warn_open,
    COUNT(*) FILTER (WHERE status NOT IN ('RESOLVED', 'IGNORED') AND severity = 'INFO') AS info_open,
    COUNT(*) FILTER (
        WHERE status NOT IN ('RESOLVED', 'IGNORED')
          AND sla_due_at IS NOT NULL
          AND sla_due_at < NOW()
    ) AS overdue_open,
    COUNT(*) FILTER (
        WHERE status NOT IN ('RESOLVED', 'IGNORED')
          AND responsible_id IS NULL
    ) AS unassigned_open
FROM kb_governance_issue;

CREATE OR REPLACE VIEW vw_kb_governance_overview_by_system AS
SELECT
    COALESCE(s.code, 'UNCLASSIFIED') AS system_code,
    COALESCE(s.name, 'Não classificado') AS system_name,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED')) AS total_open,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'ERROR') AS error_open,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'WARN') AS warn_open,
    COUNT(*) FILTER (WHERE i.status NOT IN ('RESOLVED', 'IGNORED') AND i.severity = 'INFO') AS info_open,
    COUNT(*) FILTER (
        WHERE i.status NOT IN ('RESOLVED', 'IGNORED')
          AND i.sla_due_at IS NOT NULL
          AND i.sla_due_at < NOW()
    ) AS overdue_open,
    COUNT(*) FILTER (
        WHERE i.status NOT IN ('RESOLVED', 'IGNORED')
          AND i.responsible_id IS NULL
    ) AS unassigned_open
FROM kb_governance_issue i
JOIN kb_article a ON a.id = i.article_id
LEFT JOIN kb_system s ON s.id = a.system_id
WHERE a.article_status = 1
GROUP BY COALESCE(s.code, 'UNCLASSIFIED'), COALESCE(s.name, 'Não classificado');
