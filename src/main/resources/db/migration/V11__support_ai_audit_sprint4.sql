-- =====================================================
-- KB GOVERNANCE - MIGRATION V11
-- Sprint 4: Pipeline de recorrência + AI audit + Jobs
-- =====================================================

ALTER TABLE kb_governance_issue
    ADD COLUMN IF NOT EXISTS ignored_reason VARCHAR(400);

-- ========================================
-- 1. Support tickets (Movidesk)
-- ========================================
CREATE TABLE support_ticket (
    id BIGSERIAL PRIMARY KEY,
    external_ticket_id VARCHAR(120) NOT NULL,
    protocol VARCHAR(120),
    subject VARCHAR(300),
    status VARCHAR(60),
    requester VARCHAR(150),
    owner_team VARCHAR(150),
    origin_created_at TIMESTAMPTZ,
    origin_updated_at TIMESTAMPTZ,
    last_message_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_support_ticket_external ON support_ticket(external_ticket_id);
CREATE INDEX idx_support_ticket_origin_created ON support_ticket(origin_created_at DESC);

CREATE TABLE support_ticket_message (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES support_ticket(id) ON DELETE CASCADE,
    direction VARCHAR(10) NOT NULL,
    author VARCHAR(150),
    content TEXT,
    content_html TEXT,
    external_message_key VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_support_ticket_message_key ON support_ticket_message(external_message_key);
CREATE INDEX idx_support_ticket_message_ticket ON support_ticket_message(ticket_id);

-- ========================================
-- 2. FAQ Clusters
-- ========================================
CREATE TABLE faq_cluster (
    id BIGSERIAL PRIMARY KEY,
    fingerprint VARCHAR(64) NOT NULL,
    normalized_text TEXT,
    sample_text TEXT,
    ticket_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_faq_cluster_fingerprint ON faq_cluster(fingerprint);

CREATE TABLE faq_cluster_ticket (
    id BIGSERIAL PRIMARY KEY,
    cluster_id BIGINT NOT NULL REFERENCES faq_cluster(id) ON DELETE CASCADE,
    ticket_id BIGINT NOT NULL REFERENCES support_ticket(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_faq_cluster_ticket ON faq_cluster_ticket(cluster_id, ticket_id);
CREATE INDEX idx_faq_cluster_ticket_cluster ON faq_cluster_ticket(cluster_id);

-- ========================================
-- 3. Recurrence rules + Needs
-- ========================================
CREATE TABLE recurrence_rule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    window_days INT NOT NULL,
    threshold_count INT NOT NULL,
    cooldown_hours INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE detected_need (
    id BIGSERIAL PRIMARY KEY,
    cluster_id BIGINT NOT NULL REFERENCES faq_cluster(id) ON DELETE CASCADE,
    rule_id BIGINT NOT NULL REFERENCES recurrence_rule(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    task_status VARCHAR(30),
    task_created_at TIMESTAMPTZ,
    external_ticket_id VARCHAR(120),
    last_detected_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_detected_need_cluster_rule ON detected_need(cluster_id, rule_id);
CREATE INDEX idx_detected_need_cluster ON detected_need(cluster_id);

-- ========================================
-- 4. AI readiness audit
-- ========================================
CREATE TABLE kb_article_ai_audit (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL UNIQUE REFERENCES kb_article(id) ON DELETE CASCADE,
    passed BOOLEAN NOT NULL,
    score INT NOT NULL,
    missing_sections TEXT,
    details_json JSONB,
    audited_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ========================================
-- 5. Job run audit
-- ========================================
CREATE TABLE job_run (
    id BIGSERIAL PRIMARY KEY,
    job_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    details_json JSONB
);

