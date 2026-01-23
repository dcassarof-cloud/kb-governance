-- =====================================================
-- KB GOVERNANCE - MIGRATION V7
-- Correção: avg_quality_score NUMERIC -> DOUBLE PRECISION
-- =====================================================
-- Criado em: 2026-01-23
-- Descrição: Altera tipo da coluna avg_quality_score para DOUBLE PRECISION
-- Motivo: NUMERIC(5,2) causa erro no Hibernate com tipos Double
-- =====================================================

-- Passo 1: Dropar views que dependem da coluna avg_quality_score
DROP VIEW IF EXISTS vw_ia_ready_trend CASCADE;
DROP VIEW IF EXISTS vw_monthly_comparison CASCADE;

-- Passo 2: Alterar o tipo da coluna
ALTER TABLE kb_governance_snapshot
ALTER COLUMN avg_quality_score TYPE DOUBLE PRECISION;

-- Passo 3: Recriar as views com o tipo correto

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
    AVG(avg_quality_score)::NUMERIC(10,2) AS avg_score,
    AVG(open_issues_count)::INTEGER AS avg_open_issues
FROM kb_governance_snapshot
WHERE system_code IS NULL  -- apenas global
GROUP BY DATE_TRUNC('month', snapshot_date), system_code
ORDER BY month DESC;

-- Log de confirmação
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '✅ MIGRATION V7 CONCLUÍDA';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Coluna avg_quality_score alterada para DOUBLE PRECISION';
    RAISE NOTICE 'Views recriadas: vw_ia_ready_trend, vw_monthly_comparison';
    RAISE NOTICE '========================================';
END $$;

-- =====================================================
-- FIM DA MIGRATION V7
-- =====================================================