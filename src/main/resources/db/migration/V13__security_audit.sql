-- =====================================================
-- KB GOVERNANCE - MIGRATION V13
-- Security, Auth, Audit
-- =====================================================

CREATE TABLE app_user (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    agent_id    VARCHAR(50),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_app_user_email ON app_user(email);

CREATE TABLE app_user_role (
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role    VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_token (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token       VARCHAR(200) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_refresh_token_user ON refresh_token(user_id);
CREATE INDEX ix_refresh_token_expires ON refresh_token(expires_at);

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(50),
    action          VARCHAR(200) NOT NULL,
    entity_type     VARCHAR(120),
    entity_id       VARCHAR(120),
    old_value       JSONB,
    new_value       JSONB,
    correlation_id  VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_audit_log_created_at ON audit_log(created_at DESC);
CREATE INDEX ix_audit_log_user_id ON audit_log(user_id);
CREATE INDEX ix_audit_log_entity ON audit_log(entity_type, entity_id);
