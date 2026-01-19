-- =====================================================
-- KB GOVERNANCE - MIGRATION V2
-- Governance & Sync Features
-- =====================================================
-- Criado em: 2026-01-16
-- Autor: KB Governance Team
-- Descrição: Sistema de issues, tracking de sync,
--            configuração e execuções
-- =====================================================

-- ========================================
-- 1. KB_SYNC_ISSUE
-- Issues técnicas de sincronização
-- ========================================
CREATE TABLE kb_sync_issue (
    id          BIGSERIAL PRIMARY KEY,
    
    -- Referência ao artigo (pode não existir ainda em kb_article)
    article_id  BIGINT NOT NULL,
    
    -- Tipo e Mensagem
    issue_type  VARCHAR(40) NOT NULL,  -- NOT_FOUND, EMPTY_CONTENT, MENU_NULL, MENU_NOT_MAPPED, ERROR
    message     VARCHAR(400),
    
    -- Timestamps
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Resolução
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMPTZ
);

-- Índices
CREATE INDEX ix_kb_sync_issue_article ON kb_sync_issue(article_id);
CREATE INDEX ix_kb_sync_issue_type ON kb_sync_issue(issue_type);
CREATE INDEX ix_kb_sync_issue_open ON kb_sync_issue(is_resolved) WHERE is_resolved = FALSE;

-- Constraint: Evita spam de issues iguais
CREATE UNIQUE INDEX uq_kb_sync_issue_open_unique 
    ON kb_sync_issue(article_id, issue_type) 
    WHERE is_resolved = FALSE;

-- Comentários
COMMENT ON TABLE kb_sync_issue IS 'Issues técnicas detectadas durante sincronização';
COMMENT ON INDEX uq_kb_sync_issue_open_unique IS 'Previne múltiplas issues abertas do mesmo tipo para o mesmo artigo';

-- ========================================
-- 2. KB_GOVERNANCE_ISSUE
-- Issues de qualidade/governança
-- ========================================
CREATE TABLE kb_governance_issue (
    id          BIGSERIAL PRIMARY KEY,
    
    -- Referência ao artigo
    article_id  BIGINT NOT NULL REFERENCES kb_article(id) ON DELETE CASCADE,
    
    -- Tipo e Severidade
    issue_type  VARCHAR(50) NOT NULL,  -- INCOMPLETE_CONTENT, DUPLICATE_CONTENT, INCONSISTENT_CONTENT, OUTDATED_CONTENT
    severity    VARCHAR(10) NOT NULL DEFAULT 'WARN',  -- INFO, WARN, ERROR
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN, RESOLVED
    
    -- Detalhes
    message     VARCHAR(400),
    evidence    JSONB,  -- Evidências em formato estruturado
    
    -- Timestamps
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Resolução
    resolved_at TIMESTAMPTZ,
    resolved_by VARCHAR(100)
);

-- Índices
CREATE INDEX ix_kb_gov_issue_article ON kb_governance_issue(article_id);
CREATE INDEX ix_kb_gov_issue_type ON kb_governance_issue(issue_type);
CREATE INDEX ix_kb_gov_issue_severity ON kb_governance_issue(severity);
CREATE INDEX ix_kb_gov_issue_status ON kb_governance_issue(status);
CREATE INDEX ix_kb_gov_issue_type_status ON kb_governance_issue(issue_type, status);
CREATE INDEX ix_kb_gov_issue_evidence ON kb_governance_issue USING gin(evidence);

-- Comentários
COMMENT ON TABLE kb_governance_issue IS 'Issues de qualidade e governança dos artigos';
COMMENT ON COLUMN kb_governance_issue.evidence IS 'Evidências em JSONB (ex: {textLen:123, hash:"abc"})';

-- ========================================
-- 3. KB_SYNC_CONFIG
-- Configuração do scheduler de sincronização
-- ========================================
CREATE TABLE kb_sync_config (
    id               BIGINT PRIMARY KEY DEFAULT 1,  -- Singleton
    
    -- Configuração
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    mode             VARCHAR(30) NOT NULL DEFAULT 'DELTA_WINDOW',  -- FULL, DELTA_WINDOW
    interval_minutes INTEGER NOT NULL DEFAULT 60,
    days_back        INTEGER NOT NULL DEFAULT 2,
    
    -- Tracking
    last_started_at  TIMESTAMPTZ,
    last_finished_at TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraint: Apenas 1 registro
    CHECK (id = 1)
);

-- Insere o registro singleton
INSERT INTO kb_sync_config (id) VALUES (1);

-- Comentários
COMMENT ON TABLE kb_sync_config IS 'Configuração singleton do scheduler de sincronização';
COMMENT ON COLUMN kb_sync_config.mode IS 'FULL = sync completo | DELTA_WINDOW = apenas alterados nos últimos N dias';

-- ========================================
-- 4. KB_SYNC_RUN
-- Histórico de execuções de sincronização
-- ========================================
CREATE TABLE kb_sync_run (
    id          BIGSERIAL PRIMARY KEY,
    
    -- Timestamps
    started_at  TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    duration_ms BIGINT,
    
    -- Configuração da Execução
    mode        VARCHAR(30) NOT NULL,  -- FULL, DELTA_WINDOW
    days_back   INTEGER,
    status      VARCHAR(20) NOT NULL,  -- RUNNING, SUCCESS, FAILED
    
    -- Contadores de Resultado
    synced_count     INTEGER NOT NULL DEFAULT 0,
    updated_count    INTEGER NOT NULL DEFAULT 0,
    skipped_count    INTEGER NOT NULL DEFAULT 0,
    not_found_count  INTEGER NOT NULL DEFAULT 0,
    error_count      INTEGER NOT NULL DEFAULT 0,
    
    -- Observações
    note        VARCHAR(400)
);

-- Índices
CREATE INDEX ix_kb_sync_run_started_at ON kb_sync_run(started_at DESC);
CREATE INDEX ix_kb_sync_run_status ON kb_sync_run(status);
CREATE INDEX ix_kb_sync_run_mode ON kb_sync_run(mode);

-- Comentários
COMMENT ON TABLE kb_sync_run IS 'Histórico de execuções de sincronização';
COMMENT ON COLUMN kb_sync_run.duration_ms IS 'Duração em milissegundos';

-- ========================================
-- TRIGGERS: Auto-update de timestamps
-- ========================================

-- Função genérica de update de timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para kb_governance_issue
CREATE TRIGGER trigger_kb_governance_issue_updated_at
    BEFORE UPDATE ON kb_governance_issue
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger para kb_sync_config
CREATE TRIGGER trigger_kb_sync_config_updated_at
    BEFORE UPDATE ON kb_sync_config
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- FIM DA MIGRATION V2
-- =====================================================
