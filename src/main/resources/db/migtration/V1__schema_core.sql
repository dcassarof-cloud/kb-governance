-- =====================================================
-- KB GOVERNANCE - MIGRATION V1
-- Schema Core: Tabelas Principais
-- =====================================================
-- Criado em: 2026-01-16
-- Autor: KB Governance Team
-- Descrição: Estrutura principal do sistema de governança
--            da Knowledge Base (artigos, sistemas, módulos)
-- =====================================================

-- ========================================
-- EXTENSÕES
-- ========================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ========================================
-- 1. KB_SYSTEM
-- Catálogo de sistemas/produtos da empresa
-- ========================================
CREATE TABLE kb_system (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(60) NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices
CREATE INDEX ix_kb_system_code ON kb_system(code);
CREATE INDEX ix_kb_system_active ON kb_system(is_active) WHERE is_active = TRUE;

-- Comentários
COMMENT ON TABLE kb_system IS 'Catálogo de sistemas/produtos (NotaOn, SGRH, Quinto Eixo, etc)';
COMMENT ON COLUMN kb_system.code IS 'Código único do sistema (ex: NOTAON, SGRH)';

-- ========================================
-- 2. KB_MODULE
-- Módulos dentro de sistemas (ex: Fiscal, Financeiro no ConsisaNet)
-- ========================================
CREATE TABLE kb_module (
    id          BIGSERIAL PRIMARY KEY,
    system_id   BIGINT NOT NULL REFERENCES kb_system(id) ON DELETE CASCADE,
    code        VARCHAR(60) NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE(system_id, code)
);

-- Índices
CREATE INDEX ix_kb_module_system ON kb_module(system_id);
CREATE INDEX ix_kb_module_active ON kb_module(is_active) WHERE is_active = TRUE;

-- Comentários
COMMENT ON TABLE kb_module IS 'Módulos/submódulos de sistemas (ex: Fiscal dentro do ConsisaNet)';

-- ========================================
-- 3. KB_ARTICLE
-- Artigos da Knowledge Base sincronizados do Movidesk
-- ========================================
CREATE TABLE kb_article (
    -- Identificação (usa ID do Movidesk como PK)
    id              BIGINT PRIMARY KEY,
    
    -- Conteúdo
    title           TEXT NOT NULL,
    slug            TEXT,
    summary         TEXT,
    content_html    TEXT,
    content_text    TEXT,
    
    -- Metadados do Movidesk
    article_status  INTEGER NOT NULL,
    revision_id     BIGINT,
    reading_time    VARCHAR(16),
    created_date    TIMESTAMPTZ,
    updated_date    TIMESTAMPTZ,
    
    -- Origem e Rastreabilidade
    source_system   VARCHAR(30) NOT NULL DEFAULT 'movidesk',
    source_url      TEXT NOT NULL,
    source_menu_id  BIGINT,
    source_menu_name VARCHAR(200),
    fetched_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Classificação Interna
    system_id       BIGINT REFERENCES kb_system(id) ON DELETE RESTRICT,
    module_id       BIGINT REFERENCES kb_module(id) ON DELETE SET NULL,
    
    -- Governança
    content_hash    VARCHAR(64),
    governance_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    approved_revision_id BIGINT,
    approved_at     TIMESTAMPTZ,
    
    -- Controle de Sincronização
    sync_status     VARCHAR(30) DEFAULT 'OK',
    sync_error_message VARCHAR(400),
    sync_state      VARCHAR(20),
    last_seen_at    TIMESTAMPTZ
);

-- Índices Principais
CREATE INDEX ix_kb_article_system ON kb_article(system_id);
CREATE INDEX ix_kb_article_module ON kb_article(module_id);
CREATE INDEX ix_kb_article_updated_date ON kb_article(updated_date DESC NULLS LAST);
CREATE INDEX ix_kb_article_created_date ON kb_article(created_date DESC NULLS LAST);
CREATE INDEX ix_kb_article_fetched_at ON kb_article(fetched_at DESC);

-- Índices de Governança
CREATE INDEX ix_kb_article_content_hash ON kb_article(content_hash) WHERE content_hash IS NOT NULL;
CREATE INDEX ix_kb_article_governance_status ON kb_article(governance_status);
CREATE INDEX ix_kb_article_sync_status ON kb_article(sync_status);

-- Índices de Origem
CREATE INDEX ix_kb_article_source_menu_id ON kb_article(source_menu_id);
CREATE INDEX ix_kb_article_last_seen_at ON kb_article(last_seen_at);
CREATE INDEX ix_kb_article_sync_state ON kb_article(sync_state);

-- Índice Composto para Delta Sync
CREATE INDEX ix_kb_article_delta_sync 
    ON kb_article(updated_date DESC, sync_status) 
    WHERE sync_status IS NOT NULL;

-- Comentários
COMMENT ON TABLE kb_article IS 'Artigos da Knowledge Base sincronizados do Movidesk';
COMMENT ON COLUMN kb_article.id IS 'ID do artigo no Movidesk (usado como PK)';
COMMENT ON COLUMN kb_article.content_hash IS 'SHA-256 do conteúdo normalizado (para detectar duplicados)';
COMMENT ON COLUMN kb_article.governance_status IS 'Status no fluxo de governança (PENDING, APPROVED, REJECTED)';
COMMENT ON COLUMN kb_article.sync_status IS 'Status da última sincronização (OK, NOT_FOUND, ERROR)';
COMMENT ON COLUMN kb_article.sync_state IS 'Estado no ciclo de sync (NEW, UPDATED, UNCHANGED, MISSING)';

-- ========================================
-- 4. KB_MENU_MAP
-- Mapeamento oficial: Menu Movidesk → Sistema Interno
-- ========================================
CREATE TABLE kb_menu_map (
    id               BIGSERIAL PRIMARY KEY,
    
    -- Origem (Movidesk)
    source_system    VARCHAR(30) NOT NULL DEFAULT 'movidesk',
    source_menu_id   BIGINT NOT NULL,
    source_menu_name VARCHAR(255) NOT NULL,
    
    -- Destino (Sistema Interno)
    system_id        BIGINT NOT NULL REFERENCES kb_system(id) ON DELETE CASCADE,
    
    -- Controle
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices
CREATE INDEX ix_kb_menu_map_system ON kb_menu_map(system_id);
CREATE INDEX ix_kb_menu_map_source ON kb_menu_map(source_system, source_menu_id);

-- Constraint: Apenas UM mapeamento ativo por menu
CREATE UNIQUE INDEX uq_kb_menu_map_active 
    ON kb_menu_map(source_system, source_menu_id) 
    WHERE is_active = TRUE;

-- Comentários
COMMENT ON TABLE kb_menu_map IS 'Mapeamento oficial entre menus do Movidesk e sistemas internos';
COMMENT ON INDEX uq_kb_menu_map_active IS 'Garante que cada menu tem apenas um sistema ativo';

-- =====================================================
-- FIM DA MIGRATION V1
-- =====================================================
