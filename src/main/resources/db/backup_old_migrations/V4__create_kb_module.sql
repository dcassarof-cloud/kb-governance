CREATE TABLE IF NOT EXISTS kb_module (
                                         id BIGSERIAL PRIMARY KEY,
                                         system_id BIGINT NOT NULL REFERENCES kb_system(id),
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(system_id, code)
    );

ALTER TABLE kb_article
    ADD COLUMN IF NOT EXISTS module_id BIGINT REFERENCES kb_module(id);
