-- =====================================================
-- KB GOVERNANCE - MIGRATION V4
-- Views & Reports
-- =====================================================
-- Criado em: 2026-01-16
-- Autor: KB Governance Team
-- Descrição: Views materializadas, relatórios e
--            processamento inicial de dados
-- =====================================================

-- ========================================
-- 1. FUNÇÃO: Calcular Content Hash
-- ========================================
CREATE OR REPLACE FUNCTION calculate_content_hash(content_text TEXT, content_html TEXT)
RETURNS VARCHAR(64) AS $$
BEGIN
    -- Usa content_text se disponível, senão content_html
    -- Normaliza: lowercase + remove espaços múltiplos
    -- SHA-256 do resultado
    RETURN encode(
        digest(
            convert_to(
                lower(
                    regexp_replace(
                        coalesce(nullif(trim(content_text), ''), content_html, ''),
                        '\s+',
                        ' ',
                        'g'
                    )
                ),
                'UTF8'
            ),
            'sha256'
        ),
        'hex'
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

COMMENT ON FUNCTION calculate_content_hash IS 'Calcula SHA-256 do conteúdo normalizado';

-- ========================================
-- 2. BACKFILL: Content Hash nos artigos existentes
-- ========================================
UPDATE kb_article
SET content_hash = calculate_content_hash(content_text, content_html)
WHERE content_hash IS NULL 
   OR content_hash = ''
   OR (content_text IS NOT NULL OR content_html IS NOT NULL);

-- ========================================
-- 3. VIEW: Relatório de Governança
-- ========================================
CREATE OR REPLACE VIEW kb_article_governance_report AS
SELECT
    a.id AS article_id,
    COALESCE(s.code, 'GERAL') AS system_code,
    s.name AS system_name,
    a.title,
    a.content_hash,
    a.source_url,
    a.updated_date,

    -- ============================================
    -- FLAGS DE PROBLEMAS
    -- ============================================
    
    -- 1. MANUAL_VAZIO: Sem conteúdo (HTML e TEXT vazios)
    (
        (a.content_text IS NULL OR trim(a.content_text) = '')
        AND (a.content_html IS NULL OR trim(a.content_html) = '')
    ) AS is_empty,

    -- 2. MANUAL_CURTO_DEMAIS: Conteúdo limpo < 600 caracteres
    (
        COALESCE(
            length(
                regexp_replace(
                    COALESCE(a.content_text, a.content_html, ''),
                    '\s+',
                    ' ',
                    'g'
                )
            ),
            0
        ) < 600
    ) AS is_too_short,

    -- 3. MANUAL_DUPLICADO_NO_MESMO_SISTEMA
    -- Hash repetido dentro do mesmo sistema
    (
        a.content_hash IS NOT NULL
        AND a.content_hash <> ''
        AND EXISTS (
            SELECT 1
            FROM kb_article a2
            WHERE a2.content_hash = a.content_hash
              AND a2.id <> a.id
              AND COALESCE(a2.system_id, -1) = COALESCE(a.system_id, -1)
        )
    ) AS is_duplicate_same_system,

    -- 4. HASH_REPETIDO_EM_OUTRO_SISTEMA
    -- Hash igual em sistema diferente (alerta, não erro)
    (
        a.content_hash IS NOT NULL
        AND a.content_hash <> ''
        AND EXISTS (
            SELECT 1
            FROM kb_article a2
            WHERE a2.content_hash = a.content_hash
              AND a2.id <> a.id
              AND COALESCE(a2.system_id, -1) <> COALESCE(a.system_id, -1)
        )
    ) AS is_hash_reused_other_system,

    -- 5. MANUAL_SEM_ESTRUTURA_MINIMA (critério IA-Ready)
    -- Combinação de: tamanho adequado + segmentação + orientação prática + contexto
    (
        -- Falha se conteúdo < 600 chars
        COALESCE(
            length(
                regexp_replace(
                    COALESCE(a.content_text, a.content_html, ''),
                    '\s+',
                    ' ',
                    'g'
                )
            ),
            0
        ) < 600

        -- OU não tem segmentação (headers ou passos numerados)
        OR NOT (
            a.content_html ~ '<h[123]'
            OR lower(COALESCE(a.content_text, a.content_html, '')) ~ '(passo|etapa|como fazer|como configurar)\s+\d'
        )

        -- OU não tem orientação prática (listas ou verbos de ação)
        OR NOT (
            a.content_html ~ '<(ul|ol)'
            OR lower(COALESCE(a.content_text, a.content_html, '')) ~ '(clique|acesse|selecione|preencha|confirme|cadastre|insira|abra|feche|salve|edite|delete|exclua|consulte|emita|imprima|gere|configure|ative|desative)'
        )

        -- OU não tem contexto semântico (nome do sistema/módulo)
        OR NOT (
            lower(COALESCE(a.content_text, a.content_html, '')) ~ '(notaon|nota on|consisanet|consisa net|quinto eixo|quintoeixo|sgrh|biojob|ordena|captura|edoc|cloud|edi|acor|conta shop|contashop|fiscal|financeiro|contabil|faturamento|estoque|inventario|patrimonio|caixa|protocolo|darf|cereal|escrit[oó]rio)'
        )
    ) AS lacks_min_structure,

    -- ============================================
    -- MÉTRICAS AUXILIARES
    -- ============================================
    
    -- Tamanho do conteúdo normalizado
    COALESCE(
        length(
            regexp_replace(
                COALESCE(a.content_text, a.content_html, ''),
                '\s+',
                ' ',
                'g'
            )
        ),
        0
    ) AS content_length,

    -- Quantidade de headers (h1, h2, h3)
    (
        SELECT COUNT(*)::INTEGER
        FROM regexp_matches(COALESCE(a.content_html, ''), '<h[123]', 'g')
    ) AS header_count,

    -- Tem listas?
    (a.content_html ~ '<(ul|ol)') AS has_lists,

    -- Tem verbos de ação?
    (
        lower(COALESCE(a.content_text, a.content_html, '')) ~
        '(clique|acesse|selecione|preencha|confirme|cadastre|insira|abra|feche|salve|edite|delete|exclua|consulte|emita|imprima|gere|configure|ative|desative)'
    ) AS has_action_verbs,

    -- Tem contexto de sistema?
    (
        lower(COALESCE(a.content_text, a.content_html, '')) ~
        '(notaon|nota on|consisanet|consisa net|quinto eixo|quintoeixo|sgrh|biojob|ordena|captura|edoc|cloud|edi|acor|conta shop|contashop|fiscal|financeiro|contabil|faturamento|estoque)'
    ) AS has_system_context

FROM kb_article a
LEFT JOIN kb_system s ON s.id = a.system_id
WHERE a.article_status = 1;  -- Apenas artigos ativos

-- Comentários
COMMENT ON VIEW kb_article_governance_report IS 
'Relatório automático de governança para preparação IA. 
Identifica: vazios, curtos, duplicados, sem estrutura mínima.';

-- ========================================
-- 4. VIEW: Dashboard de Métricas
-- ========================================
CREATE OR REPLACE VIEW kb_governance_dashboard AS
SELECT
    -- Totais Gerais
    (SELECT COUNT(*) FROM kb_article WHERE article_status = 1) AS total_articles,
    (SELECT COUNT(*) FROM kb_system WHERE is_active = TRUE) AS total_systems,
    (SELECT COUNT(*) FROM kb_menu_map WHERE is_active = TRUE) AS total_menu_mappings,
    
    -- Issues de Sync
    (SELECT COUNT(*) FROM kb_sync_issue WHERE is_resolved = FALSE) AS open_sync_issues,
    
    -- Issues de Governança
    (SELECT COUNT(*) FROM kb_governance_issue WHERE status = 'OPEN') AS open_governance_issues,
    
    -- Problemas por Tipo (da view de report)
    (SELECT COUNT(*) FROM kb_article_governance_report WHERE is_empty = TRUE) AS empty_articles,
    (SELECT COUNT(*) FROM kb_article_governance_report WHERE is_too_short = TRUE) AS short_articles,
    (SELECT COUNT(*) FROM kb_article_governance_report WHERE is_duplicate_same_system = TRUE) AS duplicate_articles,
    (SELECT COUNT(*) FROM kb_article_governance_report WHERE lacks_min_structure = TRUE) AS no_structure_articles,
    
    -- IA Ready
    (
        SELECT COUNT(*) 
        FROM kb_article_governance_report 
        WHERE is_empty = FALSE 
          AND is_duplicate_same_system = FALSE 
          AND lacks_min_structure = FALSE
    ) AS ia_ready_articles,
    
    -- Última Sincronização
    (SELECT MAX(started_at) FROM kb_sync_run) AS last_sync_started,
    (SELECT status FROM kb_sync_run ORDER BY started_at DESC LIMIT 1) AS last_sync_status,
    
    -- Artigos Não Classificados
    (SELECT COUNT(*) FROM kb_article WHERE system_id IS NULL AND article_status = 1) AS unclassified_articles;

COMMENT ON VIEW kb_governance_dashboard IS 'Dashboard com métricas consolidadas de governança';

-- ========================================
-- 5. ÍNDICES DE PERFORMANCE PARA VIEWS
-- ========================================

-- Índice para acelerar queries de duplicados
CREATE INDEX IF NOT EXISTS ix_kb_article_hash_system 
    ON kb_article(content_hash, system_id) 
    WHERE content_hash IS NOT NULL AND content_hash <> '';

-- Índice para artigos ativos
CREATE INDEX IF NOT EXISTS ix_kb_article_active_status 
    ON kb_article(article_status) 
    WHERE article_status = 1;

-- ========================================
-- 6. FUNÇÃO: Refresh de Estatísticas
-- ========================================
CREATE OR REPLACE FUNCTION refresh_kb_statistics()
RETURNS TABLE(
    stat_name TEXT,
    stat_value BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 'total_articles'::TEXT, COUNT(*)::BIGINT 
    FROM kb_article WHERE article_status = 1
    UNION ALL
    SELECT 'empty_articles'::TEXT, COUNT(*)::BIGINT 
    FROM kb_article_governance_report WHERE is_empty = TRUE
    UNION ALL
    SELECT 'ia_ready_articles'::TEXT, COUNT(*)::BIGINT 
    FROM kb_article_governance_report 
    WHERE is_empty = FALSE 
      AND is_duplicate_same_system = FALSE 
      AND lacks_min_structure = FALSE
    UNION ALL
    SELECT 'open_issues'::TEXT, COUNT(*)::BIGINT 
    FROM kb_governance_issue WHERE status = 'OPEN';
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION refresh_kb_statistics IS 'Retorna estatísticas atualizadas da KB';

-- ========================================
-- VALIDAÇÃO FINAL
-- ========================================
DO $$
DECLARE
    article_count INTEGER;
    view_count INTEGER;
BEGIN
    -- Verifica artigos
    SELECT COUNT(*) INTO article_count FROM kb_article;
    
    -- Verifica se view funciona
    SELECT COUNT(*) INTO view_count FROM kb_article_governance_report;
    
    RAISE NOTICE '✅ Migration V4 concluída:';
    RAISE NOTICE '   - Artigos no banco: %', article_count;
    RAISE NOTICE '   - Artigos na view de report: %', view_count;
    RAISE NOTICE '   - Views criadas: kb_article_governance_report, kb_governance_dashboard';
    RAISE NOTICE '   - Funções criadas: calculate_content_hash(), refresh_kb_statistics()';
END $$;

-- =====================================================
-- FIM DA MIGRATION V4
-- =====================================================
