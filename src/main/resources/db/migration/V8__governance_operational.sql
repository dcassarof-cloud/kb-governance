-- =====================================================
-- KB GOVERNANCE - MIGRATION V8
-- Governança Operacional (tasks, logs, notifications)
-- =====================================================
-- Criado em: 2026-02-10
-- =====================================================

-- ========================================
-- 1. KB_MANUAL_TASK
-- Estado atual de governança por manual
-- ========================================
CREATE TABLE kb_manual_task (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    risk_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    priority VARCHAR(10) NOT NULL DEFAULT 'P3',
    assignee_type VARCHAR(10),
    assignee_id VARCHAR(100),
    due_at TIMESTAMPTZ,
    ignored_reason VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (article_id) REFERENCES kb_article(id) ON DELETE CASCADE
);

-- Índices para filtros
CREATE INDEX idx_manual_task_status ON kb_manual_task(status);
CREATE INDEX idx_manual_task_risk ON kb_manual_task(risk_level);
CREATE INDEX idx_manual_task_priority ON kb_manual_task(priority);
CREATE INDEX idx_manual_task_assignee ON kb_manual_task(assignee_type, assignee_id);
CREATE INDEX idx_manual_task_due_at ON kb_manual_task(due_at) WHERE due_at IS NOT NULL;

-- ========================================
-- 2. KB_MANUAL_ACTION_LOG
-- Auditoria/histórico de ações
-- ========================================
CREATE TABLE kb_manual_action_log (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    actor_type VARCHAR(10) NOT NULL,
    actor_id VARCHAR(100),
    actor_name VARCHAR(150),
    payload_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (task_id) REFERENCES kb_manual_task(id) ON DELETE CASCADE
);

CREATE INDEX idx_manual_action_task ON kb_manual_action_log(task_id);
CREATE INDEX idx_manual_action_type ON kb_manual_action_log(action_type);
CREATE INDEX idx_manual_action_created ON kb_manual_action_log(created_at DESC);

-- ========================================
-- 3. KB_NOTIFICATION
-- Notificações in-app
-- ========================================
CREATE TABLE kb_notification (
    id BIGSERIAL PRIMARY KEY,
    recipient_type VARCHAR(10) NOT NULL,
    recipient_id VARCHAR(100) NOT NULL,
    severity VARCHAR(10) NOT NULL DEFAULT 'INFO',
    title VARCHAR(200) NOT NULL,
    message VARCHAR(500) NOT NULL,
    action_url TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_notification_recipient ON kb_notification(recipient_type, recipient_id);
CREATE INDEX idx_notification_unread ON kb_notification(is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notification_created ON kb_notification(created_at DESC);

-- ========================================
-- 4. TRIGGERS
-- Auto-update de updated_at
-- ========================================
CREATE TRIGGER trigger_kb_manual_task_updated_at
    BEFORE UPDATE ON kb_manual_task
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
