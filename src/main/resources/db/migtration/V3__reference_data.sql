-- =====================================================
-- KB GOVERNANCE - MIGRATION V3
-- Dados de Referência (Seeds)
-- =====================================================
-- Criado em: 2026-01-16
-- Autor: KB Governance Team
-- Descrição: População inicial de sistemas, módulos
--            e mapeamentos de menus
-- =====================================================

-- ========================================
-- 1. SEED: KB_SYSTEM
-- Catálogo oficial de sistemas da empresa
-- ========================================
INSERT INTO kb_system (code, name, description, is_active) VALUES
    -- Sistema Geral (fallback)
    ('GERAL', 'Geral', 'Artigos sem classificação específica ou de uso geral', TRUE),
    
    -- Sistemas Principais
    ('CONSISANET', 'ConsisaNet', 'ERP ConsisaNet (Fiscal, Financeiro, Contábil, etc)', TRUE),
    ('NOTAON', 'NotaOn', 'Sistema de emissão e gestão de notas fiscais', TRUE),
    ('SGRH', 'SGRH', 'Sistema de Gestão de Recursos Humanos', TRUE),
    ('QUINTO_EIXO', 'Quinto Eixo', 'Sistema de gestão de frotas e manutenção', TRUE),
    ('BIOJOB', 'BioJob', 'Sistema BioJob', TRUE),
    
    -- Sistemas Complementares
    ('CONTA_SHOP', 'Conta Shop', 'Plataforma Conta Shop', TRUE),
    ('ACOR', 'Açor', 'Sistema Açor', TRUE),
    ('ORDENA', 'Ordena', 'Sistema Ordena', TRUE),
    ('EDOC', 'eDoc', 'Sistema de documentos eletrônicos', TRUE),
    ('CAPTURA', 'Captura', 'Sistema de captura de documentos', TRUE),
    ('CLOUD_EDI', 'CLOUD/EDI', 'Integrações Cloud e EDI', TRUE)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = NOW();

-- ========================================
-- 2. SEED: KB_MODULE
-- Módulos do ConsisaNet
-- ========================================
INSERT INTO kb_module (system_id, code, name, description) 
SELECT 
    s.id,
    m.code,
    m.name,
    m.description
FROM kb_system s
CROSS JOIN (VALUES
    ('FISCAL', 'Fiscal', 'Módulo de gestão fiscal'),
    ('FINANCEIRO', 'Financeiro', 'Módulo de gestão financeira'),
    ('CONTABILIDADE', 'Contabilidade', 'Módulo de contabilidade'),
    ('FATURAMENTO', 'Faturamento', 'Módulo de faturamento'),
    ('DARF', 'DARF', 'Módulo de gestão de DARFs'),
    ('CEREAIS', 'Cereais', 'Módulo específico de cereais'),
    ('PATRIMONIO', 'Patrimônio', 'Módulo de gestão patrimonial'),
    ('INVENTARIO', 'Inventário', 'Módulo de inventário'),
    ('CAIXA', 'Caixa', 'Módulo de gestão de caixa'),
    ('PROTOCOLO', 'Protocolo', 'Módulo de protocolos'),
    ('ESCRITORIO', 'Escritório', 'Módulo escritório'),
    ('UTILITARIOS', 'Utilitários', 'Utilitários gerais'),
    ('INDUSTRIA', 'Indústria', 'Módulo indústria')
) AS m(code, name, description)
WHERE s.code = 'CONSISANET'
ON CONFLICT (system_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = NOW();

-- ========================================
-- 3. SEED: KB_MENU_MAP
-- Mapeamento oficial: Menu Movidesk → Sistema
-- ========================================

-- 3.1 NotaOn
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 3113, 'Menu notaon', id, TRUE
FROM kb_system WHERE code = 'NOTAON'
ON CONFLICT DO NOTHING;

-- 3.2 SGRH
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 7711, 'Menu SGRH', id, TRUE
FROM kb_system WHERE code = 'SGRH'
ON CONFLICT DO NOTHING;

-- 3.3 Quinto Eixo
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 3139, 'Menu Quinto Eixo', id, TRUE
FROM kb_system WHERE code = 'QUINTO_EIXO'
ON CONFLICT DO NOTHING;

-- 3.4 BioJob
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 18419, 'Menu Biojob', id, TRUE
FROM kb_system WHERE code = 'BIOJOB'
ON CONFLICT DO NOTHING;

-- 3.5 Conta Shop
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 7800, 'Conta Shop', id, TRUE
FROM kb_system WHERE code = 'CONTA_SHOP'
ON CONFLICT DO NOTHING;

-- 3.6 Açor
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 12085, 'Açor', id, TRUE
FROM kb_system WHERE code = 'ACOR'
ON CONFLICT DO NOTHING;

-- 3.7 Ordena
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 18628, 'Ordena', id, TRUE
FROM kb_system WHERE code = 'ORDENA'
ON CONFLICT DO NOTHING;

-- 3.8 eDoc
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 18627, 'Edoc', id, TRUE
FROM kb_system WHERE code = 'EDOC'
ON CONFLICT DO NOTHING;

-- 3.9 Captura
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 18626, 'Captura', id, TRUE
FROM kb_system WHERE code = 'CAPTURA'
ON CONFLICT DO NOTHING;

-- 3.10 CLOUD/EDI
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 18625, 'CLOUD/EDI', id, TRUE
FROM kb_system WHERE code = 'CLOUD_EDI'
ON CONFLICT DO NOTHING;

-- 3.11 ConsisaNet - Menu Geral
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', 3114, 'Menu consisanet', id, TRUE
FROM kb_system WHERE code = 'CONSISANET'
ON CONFLICT DO NOTHING;

-- 3.12 ConsisaNet - Submenus Específicos
INSERT INTO kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
SELECT 'movidesk', menu_id, menu_name, s.id, TRUE
FROM kb_system s
CROSS JOIN (VALUES
    (13281, 'Consisanet - Faturamento'),
    (13282, 'Consisanet - Fiscal'),
    (13283, 'Consisanet - Financeiro'),
    (13284, 'Consisanet - Contabilidade'),
    (18618, 'Consisanet - DARFs'),
    (18619, 'Consisanet - Cereais'),
    (18620, 'Consisanet - Patrimônio'),
    (18621, 'Consisanet - Inventários'),
    (18622, 'Consisanet - Caixa'),
    (18623, 'Consisanet - Protocolos'),
    (18624, 'Consisanet - Escritório')
) AS menus(menu_id, menu_name)
WHERE s.code = 'CONSISANET'
ON CONFLICT DO NOTHING;

-- ========================================
-- VALIDAÇÃO: Verificar mapeamentos criados
-- ========================================
DO $$
DECLARE
    total_systems INTEGER;
    total_modules INTEGER;
    total_mappings INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_systems FROM kb_system WHERE is_active = TRUE;
    SELECT COUNT(*) INTO total_modules FROM kb_module WHERE is_active = TRUE;
    SELECT COUNT(*) INTO total_mappings FROM kb_menu_map WHERE is_active = TRUE;
    
    RAISE NOTICE '✅ Seed concluído:';
    RAISE NOTICE '   - Sistemas: %', total_systems;
    RAISE NOTICE '   - Módulos: %', total_modules;
    RAISE NOTICE '   - Mapeamentos de Menu: %', total_mappings;
    
    IF total_systems < 10 THEN
        RAISE WARNING 'Menos sistemas que esperado (esperado >= 10, atual: %)', total_systems;
    END IF;
    
    IF total_mappings < 20 THEN
        RAISE WARNING 'Menos mapeamentos que esperado (esperado >= 20, atual: %)', total_mappings;
    END IF;
END $$;

-- =====================================================
-- FIM DA MIGRATION V3
-- =====================================================
