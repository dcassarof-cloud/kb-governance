-- =====================================================
-- KB GOVERNANCE - MIGRATION V6
-- Analytics Histórico + Busca Full-Text + Versionamento
-- =====================================================
-- Criado em: 2026-01-23
-- Autor: Sistema KB Governance
-- Descrição: Implementa snapshots diários, busca FTS e versionamento
-- =====================================================

-- ========================================
-- 1. TABELA: kb_governance_snapshot
-- Snapshots diários de métricas
-- ========================================

CREATE TABLE IF NOT EXISTS kb_governance_snapshot (
    id BIGSERIAL PRIMARY KEY,
    
    -- Data e Escopo
    snapshot_date DATE NOT NULL,
    system_code VARCHAR(60),  -- null = global, preenchido = sistema específico
    
    -- Métricas Principais
    total_articles INTEGER NOT NULL DEFAULT 0,
    ia_ready_count INTEGER NOT NULL DEFAULT 0,
    avg_quality_score NUMERIC(5,2) DEFAULT 0.0,
    
    -- Problemas Detectados
    empty_count INTEGER NOT NULL DEFAULT 0,
    short_count INTEGER NOT NULL DEFAULT 0,
    duplicate_count INTEGER NOT NULL DEFAULT 0,
    no_structure_count INTEGER NOT NULL DEFAULT 0,
    
    -- Issues e Atribuições
    open_issues_count INTEGER NOT NULL DEFAULT 0,
    pending_assignments INTEGER NOT NULL DEFAULT 0,
    completed_assignments INTEGER NOT NULL DEFAULT 0,
    
    -- Sync Status
    sync_ok_count INTEGER DEFAULT 0,
    sync_error_count INTEGER DEFAULT 0,
    unclassified_count INTEGER DEFAULT 0,
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices para consultas rápidas
CREATE INDEX idx_snapshot_date ON kb_governance_snapshot(snapshot_date DESC);
CREATE INDEX idx_snapshot_system ON kb_governance_snapshot(system_code);
CREATE INDEX idx_snapshot_date_system ON kb_governance_snapshot(snapshot_date, system_code);

-- Constraint: 1 snapshot por data/sistema
CREATE UNIQUE INDEX uq_snapshot_date_system 
    ON kb_governance_snapshot(snapshot_date, COALESCE(system_code, '__GLOBAL__'));

COMMENT ON TABLE kb_governance_snapshot IS 'Snapshots diários de métricas de governança';
COMMENT ON COLUMN kb_governance_snapshot.system_code IS 'null = métricas globais, preenchido = específico do sistema';

-- ========================================
-- 2. ÍNDICES FULL-TEXT SEARCH
-- PostgreSQL FTS para busca inteligente
-- ========================================

-- Extensões necessárias
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Índice GIN para full-text search em português
CREATE INDEX IF NOT EXISTS idx_kb_article_fts 
    ON kb_article 
    USING gin(
        to_tsvector('portuguese', title || ' ' || COALESCE(content_text, content_html, ''))
    );

-- Índice trigram para busca fuzzy (typos, autocomplete)
CREATE INDEX IF NOT EXISTS idx_kb_article_title_trgm 
    ON kb_article 
    USING gin(title gin_trgm_ops);

-- Índice composto para filtros + busca
CREATE INDEX IF NOT EXISTS idx_kb_article_search_filters 
    ON kb_article(article_status, system_id) 
    WHERE article_status = 1;

COMMENT ON INDEX idx_kb_article_fts IS 'Full-text search em português com stemming';
COMMENT ON INDEX idx_kb_article_title_trgm IS 'Busca fuzzy e autocomplete (trigram)';

-- ========================================
-- 3. TABELA: kb_article_version
-- Histórico de versões de artigos
-- ========================================

CREATE TABLE IF NOT EXISTS kb_article_version (
    id BIGSERIAL PRIMARY KEY,
    
    -- Referência ao Artigo
    article_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    
    -- Snapshot do Conteúdo
    title VARCHAR(500),
    summary VARCHAR(2000),
    content_text TEXT,
    content_html TEXT,
    content_hash VARCHAR(64),
    
    -- Metadata da Mudança
    changed_by VARCHAR(100),
    change_reason VARCHAR(500),
    change_type VARCHAR(30),  -- CREATED, UPDATED, APPROVED, REVERTED, SYNC
    
    -- Governança (snapshot)
    governance_status VARCHAR(30),
    quality_score INTEGER,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- FK (sem cascade, queremos preservar histórico)
    CONSTRAINT fk_version_article 
        FOREIGN KEY (article_id) 
        REFERENCES kb_article(id) 
        ON DELETE RESTRICT
);

-- Índices
CREATE INDEX idx_version_article ON kb_article_version(article_id);
CREATE INDEX idx_version_number ON kb_article_version(article_id, version_number DESC);
CREATE INDEX idx_version_created ON kb_article_version(created_at DESC);
CREATE INDEX idx_version_changed_by ON kb_article_version(changed_by);
CREATE INDEX idx_version_change_type ON kb_article_version(change_type);

-- Unique: evita duplicação de versão
CREATE UNIQUE INDEX uq_version_article_number 
    ON kb_article_version(article_id, version_number);

COMMENT ON TABLE kb_article_version IS 'Histórico completo de versões de artigos';
COMMENT ON COLUMN kb_article_version.version_number IS 'Número sequencial (1, 2, 3...)';
COMMENT ON COLUMN kb_article_version.change_type IS 'CREATED | UPDATED | APPROVED | REVERTED | SYNC';
COMMENT ON COLUMN kb_article_version.content_hash IS 'SHA-256 do conteúdo para detectar mudanças';

-- ========================================
-- 4. FUNÇÕES UTILITÁRIAS
-- ========================================

-- Função: Próximo número de versão
CREATE OR REPLACE FUNCTION get_next_version_number(p_article_id BIGINT)
RETURNS INTEGER AS $$
DECLARE
    v_max_version INTEGER;
BEGIN
    SELECT COALESCE(MAX(version_number), 0) + 1
    INTO v_max_version
    FROM kb_article_version
    WHERE article_id = p_article_id;
    
    RETURN v_max_version;
END;
$$ LANGUAGE plpgsql;

-- Função: Criar snapshot de artigo (helper)
CREATE OR REPLACE FUNCTION snapshot_article(
    p_article_id BIGINT,
    p_changed_by VARCHAR,
    p_reason TEXT,
    p_change_type VARCHAR DEFAULT 'UPDATED'
)
RETURNS BIGINT AS $$
DECLARE
    v_version_id BIGINT;
    v_version_number INTEGER;
    v_article RECORD;
BEGIN
    -- Busca artigo
    SELECT * INTO v_article
    FROM kb_article
    WHERE id = p_article_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Artigo % não encontrado', p_article_id;
    END IF;
    
    -- Próximo número de versão
    v_version_number := get_next_version_number(p_article_id);
    
    -- Cria versão
    INSERT INTO kb_article_version (
        article_id,
        version_number,
        title,
        summary,
        content_text,
        content_html,
        content_hash,
        changed_by,
        change_reason,
        change_type,
        governance_status
    ) VALUES (
        p_article_id,
        v_version_number,
        v_article.title,
        v_article.summary,
        v_article.content_text,
        v_article.content_html,
        v_article.content_hash,
        p_changed_by,
        p_reason,
        p_change_type,
        v_article.governance_status
    ) RETURNING id INTO v_version_id;
    
    RETURN v_version_id;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- 5. VIEWS ANALÍTICAS
-- ========================================

-- View: Tendência IA-Ready (últimos 30 dias)
CREATE OR REPLACE VIEW vw_ia_ready_trend AS
SELECT
    snapshot_date,
    system_code,
    total_articles,
    ia_ready_count,
    ROUND((ia_ready_count::NUMERIC / NULLIF(total_articles, 0)) * 100, 2) AS ia_ready_percentage,
    avg_quality_score,
    open_issues_count,
    pending_assignments
FROM kb_governance_snapshot
WHERE snapshot_date >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY snapshot_date DESC, system_code NULLS FIRST;

-- View: Comparação mensal
CREATE OR REPLACE VIEW vw_monthly_comparison AS
SELECT
    DATE_TRUNC('month', snapshot_date)::DATE AS month,
    system_code,
    AVG(total_articles)::INTEGER AS avg_total,
    AVG(ia_ready_count)::INTEGER AS avg_ia_ready,
    ROUND(AVG(avg_quality_score), 2) AS avg_score,
    AVG(open_issues_count)::INTEGER AS avg_open_issues
FROM kb_governance_snapshot
WHERE system_code IS NULL  -- apenas global
GROUP BY DATE_TRUNC('month', snapshot_date), system_code
ORDER BY month DESC;

-- View: Resumo de versões por artigo
CREATE OR REPLACE VIEW vw_article_version_summary AS
SELECT
    a.id AS article_id,
    a.title,
    s.code AS system_code,
    COUNT(v.id) AS version_count,
    MAX(v.version_number) AS latest_version,
    MAX(v.created_at) AS last_changed_at,
    STRING_AGG(DISTINCT v.changed_by, ', ' ORDER BY v.changed_by) AS changed_by_users
FROM kb_article a
LEFT JOIN kb_article_version v ON v.article_id = a.id
LEFT JOIN kb_system s ON s.id = a.system_id
GROUP BY a.id, a.title, s.code;

-- ========================================
-- 6. STORED PROCEDURES
-- ========================================

-- Procedure: Limpeza de snapshots antigos (manter 1 ano)
CREATE OR REPLACE PROCEDURE cleanup_old_snapshots()
LANGUAGE plpgsql
AS $$
DECLARE
    v_deleted INTEGER;
    v_cutoff_date DATE;
BEGIN
    v_cutoff_date := CURRENT_DATE - INTERVAL '1 year';
    
    DELETE FROM kb_governance_snapshot
    WHERE snapshot_date < v_cutoff_date;
    
    GET DIAGNOSTICS v_deleted = ROW_COUNT;
    
    RAISE NOTICE '🗑️ Snapshots deletados: % (anteriores a %)', v_deleted, v_cutoff_date;
END;
$$;

-- Procedure: Criar versões iniciais para artigos sem histórico
CREATE OR REPLACE PROCEDURE create_initial_versions(p_limit INTEGER DEFAULT 100)
LANGUAGE plpgsql
AS $$
DECLARE
    v_article RECORD;
    v_created INTEGER := 0;
BEGIN
    FOR v_article IN
        SELECT id
        FROM kb_article
        WHERE NOT EXISTS (
            SELECT 1
            FROM kb_article_version
            WHERE article_id = kb_article.id
        )
        LIMIT p_limit
    LOOP
        PERFORM snapshot_article(
            v_article.id,
            'SYSTEM_MIGRATION',
            'Versão inicial criada automaticamente',
            'CREATED'
        );
        
        v_created := v_created + 1;
    END LOOP;
    
    RAISE NOTICE '✅ Versões iniciais criadas: %', v_created;
END;
$$;

-- ========================================
-- 7. TRIGGERS
-- ========================================

-- Trigger: Auto-versionar ao atualizar artigo
CREATE OR REPLACE FUNCTION trigger_auto_version()
RETURNS TRIGGER AS $$
BEGIN
    -- Só cria versão se conteúdo mudou
    IF OLD.content_hash IS DISTINCT FROM NEW.content_hash THEN
        PERFORM snapshot_article(
            NEW.id,
            COALESCE(current_setting('app.current_user', true), 'SYSTEM'),
            'Atualização automática',
            'UPDATED'
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplica trigger (opcional - comentado por padrão)
-- CREATE TRIGGER trg_kb_article_auto_version
--     AFTER UPDATE ON kb_article
--     FOR EACH ROW
--     WHEN (OLD.content_hash IS DISTINCT FROM NEW.content_hash)
--     EXECUTE FUNCTION trigger_auto_version();

-- ========================================
-- 8. VALIDAÇÃO FINAL
-- ========================================

DO $$
DECLARE
    v_snapshot_exists BOOLEAN;
    v_version_exists BOOLEAN;
    v_fts_exists BOOLEAN;
BEGIN
    -- Verifica tabelas
    SELECT EXISTS (
        SELECT 1 FROM pg_tables
        WHERE tablename = 'kb_governance_snapshot'
    ) INTO v_snapshot_exists;
    
    SELECT EXISTS (
        SELECT 1 FROM pg_tables
        WHERE tablename = 'kb_article_version'
    ) INTO v_version_exists;
    
    -- Verifica índice FTS
    SELECT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'idx_kb_article_fts'
    ) INTO v_fts_exists;
    
    -- Log
    RAISE NOTICE '========================================';
    RAISE NOTICE '✅ MIGRATION V6 CONCLUÍDA';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Tabela kb_governance_snapshot: %', 
        CASE WHEN v_snapshot_exists THEN 'OK ✓' ELSE 'ERRO ✗' END;
    RAISE NOTICE 'Tabela kb_article_version: %', 
        CASE WHEN v_version_exists THEN 'OK ✓' ELSE 'ERRO ✗' END;
    RAISE NOTICE 'Índice FTS: %', 
        CASE WHEN v_fts_exists THEN 'OK ✓' ELSE 'ERRO ✗' END;
    RAISE NOTICE 'Views criadas: 3';
    RAISE NOTICE 'Stored procedures: 2';
    RAISE NOTICE 'Funções: 2';
    RAISE NOTICE '========================================';
    
    IF NOT (v_snapshot_exists AND v_version_exists AND v_fts_exists) THEN
        RAISE EXCEPTION '❌ Migration falhou! Verificar logs acima.';
    END IF;
END $$;

-- =====================================================
-- FIM DA MIGRATION V6
-- =====================================================
