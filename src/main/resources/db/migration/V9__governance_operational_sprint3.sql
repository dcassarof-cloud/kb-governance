-- =====================================================
-- KB GOVERNANCE - MIGRATION V9
-- Sprint 3: Governança Operacional (status, responsável, histórico)
-- =====================================================
-- Criado em: 2026-03-01
-- =====================================================

-- ========================================
-- 1. Corrigir duplicidade de issues por (article_id, issue_type)
-- ========================================
WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY article_id, issue_type ORDER BY created_at DESC, id DESC) AS rn
    FROM kb_governance_issue
)
DELETE FROM kb_governance_issue i
USING ranked r
WHERE i.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX uq_kb_governance_issue_article_type
    ON kb_governance_issue(article_id, issue_type);

-- ========================================
-- 2. Assignment (responsável)
-- ========================================
CREATE TABLE kb_governance_issue_assignment (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL REFERENCES kb_governance_issue(id) ON DELETE CASCADE,
    agent_id VARCHAR(100),
    agent_name VARCHAR(150),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    due_date TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gov_issue_assignment_issue ON kb_governance_issue_assignment(issue_id);
CREATE INDEX idx_gov_issue_assignment_status ON kb_governance_issue_assignment(status);
CREATE INDEX idx_gov_issue_assignment_agent ON kb_governance_issue_assignment(agent_id, agent_name);

-- ========================================
-- 3. Histórico de mudanças
-- ========================================
CREATE TABLE kb_governance_issue_history (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL REFERENCES kb_governance_issue(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    old_value VARCHAR(300),
    new_value VARCHAR(300),
    actor VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gov_issue_history_issue ON kb_governance_issue_history(issue_id);
CREATE INDEX idx_gov_issue_history_created ON kb_governance_issue_history(created_at DESC);
