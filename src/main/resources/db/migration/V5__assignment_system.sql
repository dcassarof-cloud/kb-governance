-- =====================================================
-- KB GOVERNANCE - MIGRATION V6
-- Sistema de Atribuições de Responsáveis
-- =====================================================
-- Criado em: 2026-01-21
-- Descrição: Tabelas para gerenciar atribuições de
--            artigos a agentes para atualização
-- =====================================================

-- ========================================
-- 1. KB_AGENT
-- Agentes do Movidesk que podem receber atribuições
-- ========================================
CREATE TABLE kb_agent (
    id VARCHAR(50) PRIMARY KEY,               -- ID do Movidesk
    business_name VARCHAR(200) NOT NULL,
    user_name VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Estatísticas de Produtividade
    assigned_count INTEGER NOT NULL DEFAULT 0,
    completed_count INTEGER NOT NULL DEFAULT 0,
    avg_completion_days DOUBLE PRECISION,
    
    -- Controle de Sincronização
    synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_agent_username ON kb_agent(user_name);
CREATE INDEX idx_agent_active ON kb_agent(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_agent_workload ON kb_agent(assigned_count) WHERE is_active = TRUE;

-- Comentários
COMMENT ON TABLE kb_agent IS 'Agentes do Movidesk que podem receber atribuições de atualização de artigos';
COMMENT ON COLUMN kb_agent.assigned_count IS 'Quantidade de atribuições pendentes/em andamento';
COMMENT ON COLUMN kb_agent.completed_count IS 'Total de atribuições concluídas';
COMMENT ON COLUMN kb_agent.avg_completion_days IS 'Média de dias para concluir atribuições';

-- ========================================
-- 2. KB_AGENT_TEAM
-- Times/Equipes dos agentes (relação 1:N)
-- ========================================
CREATE TABLE kb_agent_team (
    agent_id VARCHAR(50) NOT NULL,
    team_name VARCHAR(100) NOT NULL,
    
    PRIMARY KEY (agent_id, team_name),
    FOREIGN KEY (agent_id) REFERENCES kb_agent(id) ON DELETE CASCADE
);

-- Índice para busca por time
CREATE INDEX idx_agent_team_name ON kb_agent_team(team_name);

-- Comentários
COMMENT ON TABLE kb_agent_team IS 'Times/equipes dos agentes (ERP - EMPRESARIAL, FISCAL/CONTÁBIL, etc)';

-- ========================================
-- 3. KB_AGENT_SPECIALTY
-- Especialidades/Sistemas dos agentes (relação 1:N)
-- ========================================
CREATE TABLE kb_agent_specialty (
    agent_id VARCHAR(50) NOT NULL,
    system_code VARCHAR(50) NOT NULL,
    
    PRIMARY KEY (agent_id, system_code),
    FOREIGN KEY (agent_id) REFERENCES kb_agent(id) ON DELETE CASCADE
);

-- Índice para busca por especialidade
CREATE INDEX idx_agent_specialty_system ON kb_agent_specialty(system_code);

-- Comentários
COMMENT ON TABLE kb_agent_specialty IS 'Especialidades/sistemas que o agente domina (CONSISANET, NOTAON, SGRH, etc)';

-- ========================================
-- 4. KB_ARTICLE_ASSIGNMENT
-- Atribuições de artigos para atualização
-- ========================================
CREATE TABLE kb_article_assignment (
    id BIGSERIAL PRIMARY KEY,
    
    -- Referências
    article_id BIGINT NOT NULL,
    agent_id VARCHAR(50) NOT NULL,
    
    -- Ticket do Movidesk
    ticket_id VARCHAR(50),
    ticket_url TEXT,
    
    -- Detalhes da Atribuição
    reason VARCHAR(30) NOT NULL,              -- QUALITY_LOW, CONTENT_EMPTY, CONTENT_OUTDATED, DUPLICATE, NO_STRUCTURE, MANUAL_REQUEST
    description VARCHAR(500),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',  -- LOW, MEDIUM, HIGH, URGENT
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    
    -- Datas
    due_date TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    completion_note VARCHAR(500),
    
    -- Metadata
    assigned_by VARCHAR(100),                 -- Quem atribuiu (sistema ou usuário)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Foreign Keys
    FOREIGN KEY (agent_id) REFERENCES kb_agent(id) ON DELETE RESTRICT
);

-- Índices
CREATE INDEX idx_assignment_article ON kb_article_assignment(article_id);
CREATE INDEX idx_assignment_agent ON kb_article_assignment(agent_id);
CREATE INDEX idx_assignment_status ON kb_article_assignment(status);
CREATE INDEX idx_assignment_priority ON kb_article_assignment(priority);
CREATE INDEX idx_assignment_due_date ON kb_article_assignment(due_date) WHERE due_date IS NOT NULL;
CREATE INDEX idx_assignment_created ON kb_article_assignment(created_at DESC);

-- Índice composto para buscar pendentes por agente
CREATE INDEX idx_assignment_agent_active 
    ON kb_article_assignment(agent_id, status) 
    WHERE status IN ('PENDING', 'IN_PROGRESS');

-- Comentários
COMMENT ON TABLE kb_article_assignment IS 'Atribuições de artigos para agentes atualizarem';
COMMENT ON COLUMN kb_article_assignment.reason IS 'Motivo: QUALITY_LOW, CONTENT_EMPTY, CONTENT_OUTDATED, DUPLICATE, NO_STRUCTURE, MANUAL_REQUEST';
COMMENT ON COLUMN kb_article_assignment.priority IS 'Prioridade: LOW, MEDIUM, HIGH, URGENT';
COMMENT ON COLUMN kb_article_assignment.status IS 'Status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN kb_article_assignment.ticket_id IS 'ID do ticket criado no Movidesk';

-- ========================================
-- 5. TRIGGERS
-- Auto-update de timestamps
-- ========================================

-- Trigger para kb_agent
CREATE TRIGGER trigger_kb_agent_updated_at
    BEFORE UPDATE ON kb_agent
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger para kb_article_assignment
CREATE TRIGGER trigger_kb_assignment_updated_at
    BEFORE UPDATE ON kb_article_assignment
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ========================================
-- 6. SEED: DADOS INICIAIS DE AGENTES
-- ========================================

-- Seed dos 16 agentes fornecidos
INSERT INTO kb_agent (id, business_name, user_name, is_active) VALUES
    ('1363711210', 'Guilherme Cazella', 'guilherme.dellani', TRUE),
    ('154231232', 'Daniel Cassaro Filho', 'daniel.cassaro', TRUE),
    ('1790426902', 'Denis Rafael Scobar', 'denis.scobar', TRUE),
    ('1822429852', 'Tania Dias Constantino', 'tania.dias', TRUE),
    ('1974501910', 'Ariston Deluchi', 'ariston.deluchi', TRUE),
    ('2021389003', 'Oliva Patrícia Bortolini', 'oliva.bortolini', TRUE),
    ('257344081', 'Nelusa Pietraszek', 'nelusa.pietraszek', TRUE),
    ('440249170', 'Ane Caroline Almeida', 'ane.almeida', TRUE),
    ('456172540', 'Kellyn Pompermaier', 'kellyn.pompermaier', TRUE),
    ('470142134', 'Luis Carlos Brustolin', 'luis.brustolin', TRUE),
    ('525158663', 'Carlos Zatti', 'carlos.zatti', TRUE),
    ('71128321', 'Emanueli Grassi', 'emanueli.grassi', TRUE),
    ('766246855', 'Camile Paz', 'camile.paz', TRUE),
    ('797723806', 'Dione Ferreira dos Santos', 'dione.santos', TRUE),
    ('836533415', 'Cinara Piccini Capellari', 'cinara.capellari', TRUE),
    ('951745752', 'Carlos Henrique', 'carlos.candido', TRUE)
ON CONFLICT (id) DO UPDATE SET
    business_name = EXCLUDED.business_name,
    user_name = EXCLUDED.user_name,
    updated_at = NOW();

-- Seed dos times
INSERT INTO kb_agent_team (agent_id, team_name) VALUES
    ('1363711210', 'ERP - EMPRESARIAL'),
    ('154231232', 'ERP - EMPRESARIAL'),
    ('1790426902', 'ERP - EMPRESARIAL'),
    ('1822429852', 'DEPARTAMENTO PESSOAL_equ'),
    ('1974501910', 'ERP - EMPRESARIAL'),
    ('2021389003', 'FISCAL/CONTÁBIL_eq'),
    ('2021389003', 'Team Leader'),
    ('257344081', 'FISCAL/CONTÁBIL_eq'),
    ('440249170', 'FISCAL/CONTÁBIL_eq'),
    ('456172540', 'Team Leader'),
    ('456172540', 'DEPARTAMENTO PESSOAL_equ'),
    ('470142134', 'DEPARTAMENTO PESSOAL_equ'),
    ('525158663', 'FISCAL/CONTÁBIL_eq'),
    ('71128321', 'ERP - EMPRESARIAL'),
    ('71128321', 'Team Leader'),
    ('766246855', 'ERP - EMPRESARIAL'),
    ('797723806', 'FISCAL/CONTÁBIL_eq'),
    ('836533415', 'FISCAL/CONTÁBIL_eq'),
    ('951745752', 'ERP - EMPRESARIAL')
ON CONFLICT (agent_id, team_name) DO NOTHING;

-- Seed de especialidades (inferidas dos times)
INSERT INTO kb_agent_specialty (agent_id, system_code) VALUES
    -- ERP - EMPRESARIAL → CONSISANET
    ('1363711210', 'CONSISANET'),
    ('154231232', 'CONSISANET'),
    ('1790426902', 'CONSISANET'),
    ('1974501910', 'CONSISANET'),
    ('71128321', 'CONSISANET'),
    ('766246855', 'CONSISANET'),
    ('951745752', 'CONSISANET'),
    
    -- FISCAL/CONTÁBIL → CONSISANET + NOTAON
    ('2021389003', 'CONSISANET'),
    ('2021389003', 'NOTAON'),
    ('257344081', 'CONSISANET'),
    ('257344081', 'NOTAON'),
    ('440249170', 'CONSISANET'),
    ('440249170', 'NOTAON'),
    ('525158663', 'CONSISANET'),
    ('525158663', 'NOTAON'),
    ('797723806', 'CONSISANET'),
    ('797723806', 'NOTAON'),
    ('836533415', 'CONSISANET'),
    ('836533415', 'NOTAON'),
    
    -- DEPARTAMENTO PESSOAL → SGRH
    ('1822429852', 'SGRH'),
    ('456172540', 'SGRH'),
    ('470142134', 'SGRH')
ON CONFLICT (agent_id, system_code) DO NOTHING;

-- ========================================
-- 7. VIEWS ÚTEIS
-- ========================================

-- View: Agentes com estatísticas completas
CREATE OR REPLACE VIEW vw_kb_agents_full AS
SELECT
    a.id,
    a.business_name,
    a.user_name,
    a.email,
    a.is_active,
    a.assigned_count,
    a.completed_count,
    a.avg_completion_days,
    
    -- Times (agregados em array)
    COALESCE(
        array_agg(DISTINCT t.team_name) FILTER (WHERE t.team_name IS NOT NULL),
        ARRAY[]::VARCHAR[]
    ) AS teams,
    
    -- Especialidades (agregadas em array)
    COALESCE(
        array_agg(DISTINCT s.system_code) FILTER (WHERE s.system_code IS NOT NULL),
        ARRAY[]::VARCHAR[]
    ) AS specialties,
    
    a.synced_at,
    a.created_at,
    a.updated_at
FROM kb_agent a
LEFT JOIN kb_agent_team t ON t.agent_id = a.id
LEFT JOIN kb_agent_specialty s ON s.agent_id = a.id
GROUP BY a.id, a.business_name, a.user_name, a.email, a.is_active,
         a.assigned_count, a.completed_count, a.avg_completion_days,
         a.synced_at, a.created_at, a.updated_at;

-- View: Atribuições com informações completas
CREATE OR REPLACE VIEW vw_kb_assignments_full AS
SELECT
    asn.id,
    asn.article_id,
    art.title AS article_title,
    sys.code AS system_code,
    sys.name AS system_name,
    
    asn.agent_id,
    ag.business_name AS agent_name,
    ag.user_name AS agent_username,
    
    asn.ticket_id,
    asn.ticket_url,
    asn.reason,
    asn.description,
    asn.priority,
    asn.status,
    
    asn.due_date,
    asn.completed_at,
    asn.completion_note,
    asn.assigned_by,
    asn.created_at,
    asn.updated_at,
    
    -- Campos calculados
    CASE
        WHEN asn.status = 'COMPLETED' THEN NULL
        WHEN asn.due_date IS NULL THEN FALSE
        ELSE asn.due_date < NOW()
    END AS is_overdue,
    
    CASE
        WHEN asn.completed_at IS NOT NULL
        THEN EXTRACT(EPOCH FROM (asn.completed_at - asn.created_at)) / 86400
        ELSE NULL
    END AS completion_days
    
FROM kb_article_assignment asn
JOIN kb_agent ag ON ag.id = asn.agent_id
LEFT JOIN kb_article art ON art.id = asn.article_id
LEFT JOIN kb_system sys ON sys.id = art.system_id;

-- View: Dashboard de atribuições
CREATE OR REPLACE VIEW vw_kb_assignment_dashboard AS
SELECT
    COUNT(*) AS total_assignments,
    
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count,
    SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress_count,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count,
    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_count,
    
    SUM(CASE 
        WHEN status IN ('PENDING', 'IN_PROGRESS') AND due_date < NOW() 
        THEN 1 ELSE 0 
    END) AS overdue_count,
    
    AVG(CASE 
        WHEN completed_at IS NOT NULL 
        THEN EXTRACT(EPOCH FROM (completed_at - created_at)) / 86400 
        ELSE NULL 
    END) AS avg_completion_days,
    
    COUNT(DISTINCT agent_id) AS active_agents
    
FROM kb_article_assignment;

-- ========================================
-- 8. STORED PROCEDURES
-- ========================================

-- Procedure: Atualizar estatísticas de agentes
CREATE OR REPLACE PROCEDURE sp_update_agent_stats()
LANGUAGE plpgsql
AS $$
BEGIN
    -- Atualiza contadores de atribuições ativas
    UPDATE kb_agent a
    SET assigned_count = (
        SELECT COUNT(*)
        FROM kb_article_assignment asn
        WHERE asn.agent_id = a.id
          AND asn.status IN ('PENDING', 'IN_PROGRESS')
    );
    
    -- Atualiza contadores de concluídas
    UPDATE kb_agent a
    SET completed_count = (
        SELECT COUNT(*)
        FROM kb_article_assignment asn
        WHERE asn.agent_id = a.id
          AND asn.status = 'COMPLETED'
    );
    
    -- Atualiza média de dias para conclusão
    UPDATE kb_agent a
    SET avg_completion_days = (
        SELECT AVG(EXTRACT(EPOCH FROM (asn.completed_at - asn.created_at)) / 86400)
        FROM kb_article_assignment asn
        WHERE asn.agent_id = a.id
          AND asn.status = 'COMPLETED'
          AND asn.completed_at IS NOT NULL
    );
    
    RAISE NOTICE '✅ Estatísticas de agentes atualizadas';
END;
$$;

-- Procedure: Marcar atribuições atrasadas
CREATE OR REPLACE PROCEDURE sp_notify_overdue_assignments()
LANGUAGE plpgsql
AS $$
DECLARE
    v_overdue_count INTEGER;
BEGIN
    -- Conta atribuições atrasadas
    SELECT COUNT(*) INTO v_overdue_count
    FROM kb_article_assignment
    WHERE status IN ('PENDING', 'IN_PROGRESS')
      AND due_date < NOW();
    
    IF v_overdue_count > 0 THEN
        RAISE NOTICE '⚠️ % atribuições atrasadas encontradas', v_overdue_count;
    ELSE
        RAISE NOTICE '✅ Nenhuma atribuição atrasada';
    END IF;
END;
$$;

-- ========================================
-- VALIDAÇÃO FINAL
-- ========================================

DO $$
DECLARE
    agent_count INTEGER;
    team_count INTEGER;
    specialty_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO agent_count FROM kb_agent;
    SELECT COUNT(*) INTO team_count FROM kb_agent_team;
    SELECT COUNT(*) INTO specialty_count FROM kb_agent_specialty;
    
    RAISE NOTICE '✅ Migration V6 concluída:';
    RAISE NOTICE '   - Agentes importados: %', agent_count;
    RAISE NOTICE '   - Times mapeados: %', team_count;
    RAISE NOTICE '   - Especialidades configuradas: %', specialty_count;
    
    IF agent_count < 16 THEN
        RAISE WARNING '⚠️ Esperados 16 agentes, mas foram importados apenas %', agent_count;
    END IF;
END $$;
