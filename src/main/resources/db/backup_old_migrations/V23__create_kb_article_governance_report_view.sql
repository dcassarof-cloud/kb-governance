-- =====================================================
-- VIEW: kb_article_governance_report
-- Relatório de governança para preparação IA
-- =====================================================

create or replace view kb_article_governance_report as
select
    a.id as article_id,
    coalesce(s.code, 'GERAL') as system_code,
    s.name as system_name,
    a.title,
    a.content_hash,
    a.source_url,
    a.updated_date,

    -- ============================================
    -- 1️⃣ MANUAL_VAZIO
    -- ============================================
    (
        (a.content_text is null or trim(a.content_text) = '')
            and (a.content_html is null or trim(a.content_html) = '')
        ) as is_empty,

    -- ============================================
    -- 2️⃣ MANUAL_CURTO_DEMAIS
    -- Texto limpo < 600 caracteres
    -- ============================================
    (
        coalesce(
                length(
                        regexp_replace(
                                coalesce(a.content_text, a.content_html, ''),
                                '\s+',
                                ' ',
                                'g'
                        )
                ),
                0
        ) < 600
        ) as is_too_short,

    -- ============================================
    -- 3️⃣ MANUAL_DUPLICADO_NO_MESMO_SISTEMA
    -- Hash repetido APENAS dentro do mesmo sistema
    -- ============================================
    (
        a.content_hash is not null
            and a.content_hash <> ''
            and exists (
            select 1
            from kb_article a2
            where a2.content_hash = a.content_hash
              and a2.id <> a.id
              and coalesce(a2.system_id, -1) = coalesce(a.system_id, -1)
        )
        ) as is_duplicate_same_system,

    -- ============================================
    -- 4️⃣ HASH_REPETIDO_EM_OUTRO_SISTEMA (alerta)
    -- Hash igual em sistema diferente
    -- ============================================
    (
        a.content_hash is not null
            and a.content_hash <> ''
            and exists (
            select 1
            from kb_article a2
            where a2.content_hash = a.content_hash
              and a2.id <> a.id
              and coalesce(a2.system_id, -1) <> coalesce(a.system_id, -1)
        )
        ) as is_hash_reused_other_system,

    -- ============================================
    -- 5️⃣ MANUAL_SEM_ESTRUTURA_MINIMA (IA-Ready)
    -- ============================================
    -- Critério 1: Conteúdo >= 600 chars (já verificado em is_too_short)
    -- Critério 2: Segmentação (headings ou passos)
    -- Critério 3: Orientação prática (listas ou verbos de ação)
    -- Critério 4: Contexto semântico (nome do sistema/módulo)
    (
        -- Falha se conteúdo < 600
        coalesce(
                length(
                        regexp_replace(
                                coalesce(a.content_text, a.content_html, ''),
                                '\s+',
                                ' ',
                                'g'
                        )
                ),
                0
        ) < 600

            -- OU não tem segmentação (headers ou passos)
            or not (
            -- Tem headers HTML
            a.content_html ~ '<h[123]'
            -- OU tem padrão de passos
            or lower(coalesce(a.content_text, a.content_html, '')) ~ '(passo|etapa|como fazer|como configurar)\s+\d'
            )

            -- OU não tem orientação prática
            or not (
            -- Tem listas
            a.content_html ~ '<(ul|ol)'
            -- OU tem verbos de ação
            or lower(coalesce(a.content_text, a.content_html, '')) ~ '(clique|acesse|selecione|preencha|confirme|cadastre|insira|abra|feche|salve|edite|delete|exclua|consulte|emita|imprima|gere|configure|ative|desative)'
            )

            -- OU não tem contexto semântico
            or not (
            -- Nome do sistema no conteúdo
            lower(coalesce(a.content_text, a.content_html, '')) ~ '(notaon|nota on|consisanet|consisa net|quinto eixo|quintoeixo|sgrh|biojob|ordena|captura|edoc|cloud|edi|acor|conta shop|contashop)'
            -- OU tem nome de módulo comum
            or lower(coalesce(a.content_text, a.content_html, '')) ~ '(fiscal|financeiro|contabil|faturamento|estoque|inventario|patrimonio|caixa|protocolo|darf|cereal|escrit[oó]rio)'
            )
        ) as lacks_min_structure,

    -- ============================================
    -- MÉTRICAS AUXILIARES
    -- ============================================
    coalesce(
            length(
                    regexp_replace(
                            coalesce(a.content_text, a.content_html, ''),
                            '\s+',
                            ' ',
                            'g'
                    )
            ),
            0
    ) as content_length,

    -- Conta headers
    (
        select count(*)
        from regexp_matches(a.content_html, '<h[123]', 'g')
    ) as header_count,

    -- Tem listas?
    (a.content_html ~ '<(ul|ol)') as has_lists,

    -- Tem verbos de ação?
    (
        lower(coalesce(a.content_text, a.content_html, '')) ~
        '(clique|acesse|selecione|preencha|confirme|cadastre|insira|abra|feche|salve|edite|delete|exclua|consulte|emita|imprima|gere|configure|ative|desative)'
        ) as has_action_verbs,

    -- Tem contexto de sistema?
    (
        lower(coalesce(a.content_text, a.content_html, '')) ~
        '(notaon|nota on|consisanet|consisa net|quinto eixo|quintoeixo|sgrh|biojob|ordena|captura|edoc|cloud|edi|acor|conta shop|contashop|fiscal|financeiro|contabil|faturamento|estoque)'
        ) as has_system_context

from kb_article a
         left join kb_system s on s.id = a.system_id
where a.article_status = 1; -- apenas artigos ativos

-- =====================================================
-- ÍNDICE NA VIEW MATERIALIZADA (opcional, para performance)
-- =====================================================
-- Se quiser cache, pode criar uma materialized view:
-- create materialized view kb_article_governance_report_cached as ...
-- create index on kb_article_governance_report_cached(article_id);
-- create index on kb_article_governance_report_cached(system_code);

-- =====================================================
-- COMENTÁRIOS
-- =====================================================
comment on view kb_article_governance_report is
'Relatório automático de governança dos manuais para preparação IA.
Identifica: vazios, curtos, duplicados, sem estrutura mínima.';