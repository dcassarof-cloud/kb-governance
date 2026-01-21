-- =====================================================
-- KB GOVERNANCE - SCHEMA COMPLETO
-- Versão: 2.0 - Advanced Governance
-- =====================================================

-- =====================================================
-- 1. CATEGORIZAÇÃO E TAGS
-- =====================================================

-- Tabela de categorias disponíveis
CREATE TABLE kb_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id BIGINT,
    color VARCHAR(7), -- HEX color: #0054C7
    icon VARCHAR(50),
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES kb_categories(id)
);

-- Índices para performance
CREATE INDEX idx_category_code ON kb_categories(code);
CREATE INDEX idx_category_active ON kb_categories(is_active);

-- Dados iniciais de categorias
INSERT INTO kb_categories (code, name, description, color, icon, display_order) VALUES
('FINANCEIRO', 'Financeiro', 'Contas a pagar, receber, faturamento', '#10b981', '💰', 1),
('RH', 'Recursos Humanos', 'Folha, benefícios, ponto', '#f59e0b', '👥', 2),
('PRODUCAO', 'Produção', 'Ordens de produção, planejamento', '#3b82f6', '🏭', 3),
('VENDAS', 'Vendas', 'Pedidos, propostas, CRM', '#8b5cf6', '📊', 4),
('COMPRAS', 'Compras', 'Cotações, pedidos de compra', '#ef4444', '🛒', 5),
('ESTOQUE', 'Estoque', 'Movimentações, inventário', '#6366f1', '📦', 6),
('FISCAL', 'Fiscal', 'Notas fiscais, impostos', '#ec4899', '📋', 7),
('SUPORTE', 'Suporte Técnico', 'Configurações, manutenção', '#14b8a6', '🔧', 8),
('GERENCIAL', 'Gerencial', 'Dashboards, relatórios', '#f97316', '📈', 9),
('OUTROS', 'Outros', 'Diversos', '#6b7280', '📁', 10);

-- =====================================================
-- 2. ARTIGOS COM CATEGORIZAÇÃO
-- =====================================================

-- Tabela principal de artigos (extendida)
CREATE TABLE kb_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id VARCHAR(100) UNIQUE NOT NULL, -- ID do Movidesk
    title VARCHAR(500),
    content_text TEXT,
    content_html TEXT,

    -- Categorização
    system_code VARCHAR(50), -- SIENGE, MOBUSS, etc
    category_id BIGINT,
    subcategory_id BIGINT,
    tags JSON, -- ["tag1", "tag2", "tag3"]

    -- Workflow
    status VARCHAR(20) DEFAULT 'PUBLISHED', -- DRAFT, REVIEW, APPROVED, PUBLISHED, ARCHIVED
    workflow_stage VARCHAR(50),

    -- Responsabilidade
    author_id VARCHAR(100),
    author_name VARCHAR(200),
    reviewer_id VARCHAR(100),
    reviewer_name VARCHAR(200),
    approver_id VARCHAR(100),
    approver_name VARCHAR(200),

    -- Qualidade
    quality_score INT DEFAULT 0,
    ia_ready BOOLEAN DEFAULT FALSE,

    -- Flags de problemas
    is_empty BOOLEAN DEFAULT FALSE,
    is_too_short BOOLEAN DEFAULT FALSE,
    is_duplicate_same_system BOOLEAN DEFAULT FALSE,
    lacks_min_structure BOOLEAN DEFAULT FALSE,

    -- Versioning
    version INT DEFAULT 1,
    previous_version_id BIGINT,

    -- Metadados
    source_url TEXT,
    published_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    approved_at TIMESTAMP,
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (category_id) REFERENCES kb_categories(id),
    FOREIGN KEY (subcategory_id) REFERENCES kb_categories(id),
    FOREIGN KEY (previous_version_id) REFERENCES kb_articles(id)
);

-- Índices para performance
CREATE INDEX idx_article_id ON kb_articles(article_id);
CREATE INDEX idx_system_code ON kb_articles(system_code);
CREATE INDEX idx_category ON kb_articles(category_id);
CREATE INDEX idx_status ON kb_articles(status);
CREATE INDEX idx_quality_score ON kb_articles(quality_score);
CREATE INDEX idx_ia_ready ON kb_articles(ia_ready);

-- =====================================================
-- 3. HISTÓRICO DE QUALIDADE (MÉTRICAS AO LONGO DO TEMPO)
-- =====================================================

CREATE TABLE kb_quality_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,

    -- Snapshot da qualidade
    quality_score INT NOT NULL,
    ia_ready BOOLEAN DEFAULT FALSE,

    -- Problemas detectados
    is_empty BOOLEAN DEFAULT FALSE,
    is_too_short BOOLEAN DEFAULT FALSE,
    is_duplicate BOOLEAN DEFAULT FALSE,
    lacks_structure BOOLEAN DEFAULT FALSE,

    -- Métricas
    content_length INT DEFAULT 0,
    word_count INT DEFAULT 0,

    -- Metadados
    measured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    triggered_by VARCHAR(50), -- SYNC, MANUAL_EDIT, REVIEW, SCHEDULED

    FOREIGN KEY (article_id) REFERENCES kb_articles(id) ON DELETE CASCADE
);

-- Índices
CREATE INDEX idx_quality_article ON kb_quality_history(article_id);
CREATE INDEX idx_quality_measured ON kb_quality_history(measured_at);
CREATE INDEX idx_quality_score ON kb_quality_history(quality_score);

-- =====================================================
-- 4. WORKFLOW DE APROVAÇÃO
-- =====================================================

CREATE TABLE kb_workflow_transitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,

    -- Transição
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,

    -- Autor da transição
    user_id VARCHAR(100),
    user_name VARCHAR(200),
    user_email VARCHAR(200),

    -- Detalhes
    comment TEXT,
    reason VARCHAR(50), -- APPROVED, REJECTED, NEEDS_REVISION, PUBLISHED

    -- Metadata
    transitioned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (article_id) REFERENCES kb_articles(id) ON DELETE CASCADE
);

-- Índices
CREATE INDEX idx_workflow_article ON kb_workflow_transitions(article_id);
CREATE INDEX idx_workflow_status ON kb_workflow_transitions(to_status);
CREATE INDEX idx_workflow_date ON kb_workflow_transitions(transitioned_at);

-- =====================================================
-- 5. ALERTAS E NOTIFICAÇÕES
-- =====================================================

CREATE TABLE kb_quality_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,

    -- Tipo de alerta
    alert_type VARCHAR(50) NOT NULL, -- QUALITY_DROP, CRITICAL_SCORE, EMPTY_CONTENT, DUPLICATE_DETECTED, REVIEW_NEEDED
    severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL

    -- Detalhes do alerta
    title VARCHAR(200) NOT NULL,
    description TEXT,

    -- Valores comparativos
    previous_score INT,
    current_score INT,
    threshold_value INT,

    -- Status do alerta
    status VARCHAR(20) DEFAULT 'OPEN', -- OPEN, ACKNOWLEDGED, RESOLVED, IGNORED
    acknowledged_by VARCHAR(100),
    acknowledged_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP,
    resolution_comment TEXT,

    -- Notificações enviadas
    notification_sent BOOLEAN DEFAULT FALSE,
    notification_channels JSON, -- ["email", "slack", "dashboard"]

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (article_id) REFERENCES kb_articles(id) ON DELETE CASCADE
);

-- Índices
CREATE INDEX idx_alert_article ON kb_quality_alerts(article_id);
CREATE INDEX idx_alert_type ON kb_quality_alerts(alert_type);
CREATE INDEX idx_alert_severity ON kb_quality_alerts(severity);
CREATE INDEX idx_alert_status ON kb_quality_alerts(status);
CREATE INDEX idx_alert_created ON kb_quality_alerts(created_at);

-- =====================================================
-- 6. MÉTRICAS AGREGADAS POR CATEGORIA
-- =====================================================

CREATE TABLE kb_category_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    system_code VARCHAR(50),

    -- Contadores
    total_articles INT DEFAULT 0,
    ia_ready_count INT DEFAULT 0,
    draft_count INT DEFAULT 0,
    review_count INT DEFAULT 0,
    approved_count INT DEFAULT 0,

    -- Problemas
    empty_count INT DEFAULT 0,
    short_count INT DEFAULT 0,
    duplicate_count INT DEFAULT 0,
    no_structure_count INT DEFAULT 0,

    -- Qualidade
    avg_quality_score DECIMAL(5,2) DEFAULT 0,
    min_quality_score INT DEFAULT 0,
    max_quality_score INT DEFAULT 0,

    -- Alertas
    open_alerts_count INT DEFAULT 0,
    critical_alerts_count INT DEFAULT 0,

    -- Snapshot temporal
    snapshot_date DATE NOT NULL,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (category_id) REFERENCES kb_categories(id),
    UNIQUE KEY uk_category_system_date (category_id, system_code, snapshot_date)
);

-- Índices
CREATE INDEX idx_metrics_category ON kb_category_metrics(category_id);
CREATE INDEX idx_metrics_system ON kb_category_metrics(system_code);
CREATE INDEX idx_metrics_date ON kb_category_metrics(snapshot_date);

-- =====================================================
-- 7. CONFIGURAÇÕES DE ALERTAS
-- =====================================================

CREATE TABLE kb_alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Identificação
    rule_name VARCHAR(100) NOT NULL,
    rule_code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,

    -- Condições
    alert_type VARCHAR(50) NOT NULL,
    condition_field VARCHAR(50) NOT NULL, -- quality_score, ia_ready, status
    condition_operator VARCHAR(20) NOT NULL, -- LT, GT, EQ, NEQ, BETWEEN
    condition_value VARCHAR(100) NOT NULL,

    -- Severidade
    severity VARCHAR(20) NOT NULL,

    -- Filtros (opcional)
    category_filter JSON, -- ["FINANCEIRO", "RH"]
    system_filter JSON, -- ["SIENGE", "MOBUSS"]

    -- Notificações
    send_email BOOLEAN DEFAULT FALSE,
    email_recipients JSON,
    send_slack BOOLEAN DEFAULT FALSE,
    slack_channels JSON,
    send_dashboard BOOLEAN DEFAULT TRUE,

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    -- Metadata
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Regras padrão
INSERT INTO kb_alert_rules (rule_name, rule_code, alert_type, condition_field, condition_operator, condition_value, severity, send_dashboard) VALUES
('Queda Crítica de Qualidade', 'QUALITY_DROP_CRITICAL', 'QUALITY_DROP', 'quality_score', 'LT', '30', 'CRITICAL', TRUE),
('Queda Moderada de Qualidade', 'QUALITY_DROP_MODERATE', 'QUALITY_DROP', 'quality_score', 'BETWEEN', '30:50', 'HIGH', TRUE),
('Artigo Vazio Detectado', 'EMPTY_CONTENT', 'EMPTY_CONTENT', 'is_empty', 'EQ', 'true', 'HIGH', TRUE),
('Duplicata Detectada', 'DUPLICATE_DETECTED', 'DUPLICATE_DETECTED', 'is_duplicate', 'EQ', 'true', 'MEDIUM', TRUE),
('Aguardando Revisão (+7 dias)', 'REVIEW_OVERDUE', 'REVIEW_NEEDED', 'status', 'EQ', 'REVIEW', 'MEDIUM', TRUE);

-- =====================================================
-- 8. VIEWS ÚTEIS
-- =====================================================

-- View: Artigos com informações completas
CREATE OR REPLACE VIEW vw_kb_articles_full AS
SELECT
    a.*,
    c.name AS category_name,
    c.color AS category_color,
    sc.name AS subcategory_name,
    (SELECT COUNT(*) FROM kb_quality_alerts WHERE article_id = a.id AND status = 'OPEN') AS open_alerts,
    (SELECT COUNT(*) FROM kb_workflow_transitions WHERE article_id = a.id) AS workflow_transitions_count,
    (SELECT quality_score FROM kb_quality_history WHERE article_id = a.id ORDER BY measured_at DESC LIMIT 1) AS latest_quality_score
FROM kb_articles a
LEFT JOIN kb_categories c ON a.category_id = c.id
LEFT JOIN kb_categories sc ON a.subcategory_id = sc.id;

-- View: Métricas por categoria (hoje)
CREATE OR REPLACE VIEW vw_category_metrics_today AS
SELECT
    c.id AS category_id,
    c.code AS category_code,
    c.name AS category_name,
    c.color AS category_color,
    COUNT(a.id) AS total_articles,
    SUM(CASE WHEN a.ia_ready = TRUE THEN 1 ELSE 0 END) AS ia_ready_count,
    SUM(CASE WHEN a.status = 'DRAFT' THEN 1 ELSE 0 END) AS draft_count,
    SUM(CASE WHEN a.status = 'REVIEW' THEN 1 ELSE 0 END) AS review_count,
    SUM(CASE WHEN a.status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count,
    AVG(a.quality_score) AS avg_quality_score,
    SUM(CASE WHEN a.is_empty = TRUE THEN 1 ELSE 0 END) AS empty_count,
    SUM(CASE WHEN a.is_too_short = TRUE THEN 1 ELSE 0 END) AS short_count,
    (SELECT COUNT(*) FROM kb_quality_alerts al WHERE al.article_id IN (SELECT id FROM kb_articles WHERE category_id = c.id) AND al.status = 'OPEN') AS open_alerts
FROM kb_categories c
LEFT JOIN kb_articles a ON a.category_id = c.id
WHERE c.is_active = TRUE
GROUP BY c.id, c.code, c.name, c.color;

-- View: Alertas ativos com detalhes
CREATE OR REPLACE VIEW vw_active_alerts AS
SELECT
    al.*,
    a.article_id AS movidesk_id,
    a.title AS article_title,
    a.system_code,
    c.name AS category_name,
    a.quality_score AS current_quality
FROM kb_quality_alerts al
JOIN kb_articles a ON al.article_id = a.id
LEFT JOIN kb_categories c ON a.category_id = c.id
WHERE al.status = 'OPEN'
ORDER BY al.severity DESC, al.created_at DESC;

-- =====================================================
-- 9. STORED PROCEDURES
-- =====================================================

DELIMITER //

-- Procedure: Calcular e registrar qualidade
CREATE PROCEDURE sp_calculate_article_quality(IN p_article_id BIGINT, IN p_triggered_by VARCHAR(50))
BEGIN
    DECLARE v_quality_score INT;
    DECLARE v_ia_ready BOOLEAN;
    DECLARE v_is_empty BOOLEAN;
    DECLARE v_is_short BOOLEAN;
    DECLARE v_lacks_structure BOOLEAN;
    DECLARE v_content_length INT;
    DECLARE v_word_count INT;

    -- Buscar dados do artigo
    SELECT
        quality_score,
        ia_ready,
        is_empty,
        is_too_short,
        lacks_min_structure,
        CHAR_LENGTH(content_text),
        (CHAR_LENGTH(content_text) - CHAR_LENGTH(REPLACE(content_text, ' ', '')) + 1)
    INTO
        v_quality_score,
        v_ia_ready,
        v_is_empty,
        v_is_short,
        v_lacks_structure,
        v_content_length,
        v_word_count
    FROM kb_articles
    WHERE id = p_article_id;

    -- Registrar histórico
    INSERT INTO kb_quality_history (
        article_id, quality_score, ia_ready, is_empty, is_too_short,
        lacks_structure, content_length, word_count, triggered_by
    ) VALUES (
        p_article_id, v_quality_score, v_ia_ready, v_is_empty, v_is_short,
        v_lacks_structure, v_content_length, v_word_count, p_triggered_by
    );

    -- Verificar se deve gerar alertas
    CALL sp_check_quality_alerts(p_article_id, v_quality_score);
END //

-- Procedure: Verificar e gerar alertas
CREATE PROCEDURE sp_check_quality_alerts(IN p_article_id BIGINT, IN p_current_score INT)
BEGIN
    DECLARE v_previous_score INT;
    DECLARE v_score_drop INT;

    -- Buscar score anterior
    SELECT quality_score INTO v_previous_score
    FROM kb_quality_history
    WHERE article_id = p_article_id
    ORDER BY measured_at DESC
    LIMIT 1 OFFSET 1;

    -- Calcular queda
    SET v_score_drop = COALESCE(v_previous_score, p_current_score) - p_current_score;

    -- Alerta: Queda crítica (>20 pontos)
    IF v_score_drop > 20 THEN
        INSERT INTO kb_quality_alerts (
            article_id, alert_type, severity, title, description,
            previous_score, current_score, threshold_value
        ) VALUES (
            p_article_id, 'QUALITY_DROP', 'CRITICAL',
            'Queda Crítica de Qualidade Detectada',
            CONCAT('O score caiu de ', v_previous_score, ' para ', p_current_score, ' (-', v_score_drop, ' pontos)'),
            v_previous_score, p_current_score, 20
        );
    END IF;

    -- Alerta: Score crítico (<30)
    IF p_current_score < 30 THEN
        INSERT INTO kb_quality_alerts (
            article_id, alert_type, severity, title, description,
            current_score, threshold_value
        ) VALUES (
            p_article_id, 'CRITICAL_SCORE', 'CRITICAL',
            'Score Crítico Detectado',
            CONCAT('Artigo com score muito baixo: ', p_current_score, ' (limite: 30)'),
            p_current_score, 30
        );
    END IF;
END //

-- Procedure: Transição de workflow
CREATE PROCEDURE sp_workflow_transition(
    IN p_article_id BIGINT,
    IN p_to_status VARCHAR(20),
    IN p_user_id VARCHAR(100),
    IN p_user_name VARCHAR(200),
    IN p_comment TEXT,
    IN p_reason VARCHAR(50)
)
BEGIN
    DECLARE v_from_status VARCHAR(20);

    -- Buscar status atual
    SELECT status INTO v_from_status
    FROM kb_articles
    WHERE id = p_article_id;

    -- Atualizar status do artigo
    UPDATE kb_articles
    SET
        status = p_to_status,
        reviewed_at = CASE WHEN p_to_status = 'REVIEW' THEN NOW() ELSE reviewed_at END,
        approved_at = CASE WHEN p_to_status = 'APPROVED' THEN NOW() ELSE approved_at END,
        reviewer_id = CASE WHEN p_to_status = 'REVIEW' THEN p_user_id ELSE reviewer_id END,
        reviewer_name = CASE WHEN p_to_status = 'REVIEW' THEN p_user_name ELSE reviewer_name END,
        approver_id = CASE WHEN p_to_status = 'APPROVED' THEN p_user_id ELSE approver_id END,
        approver_name = CASE WHEN p_to_status = 'APPROVED' THEN p_user_name ELSE approver_name END
    WHERE id = p_article_id;

    -- Registrar transição
    INSERT INTO kb_workflow_transitions (
        article_id, from_status, to_status, user_id, user_name, comment, reason
    ) VALUES (
        p_article_id, v_from_status, p_to_status, p_user_id, p_user_name, p_comment, p_reason
    );
END //

DELIMITER ;

-- =====================================================
-- FIM DO SCHEMA
-- =====================================================