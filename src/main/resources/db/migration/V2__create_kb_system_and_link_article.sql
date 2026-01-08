-- V2__create_kb_system_and_link_article.sql

-- 1) Tabela de sistemas/módulos
CREATE TABLE IF NOT EXISTS kb_system (
                                         id          BIGSERIAL PRIMARY KEY,
                                         code        VARCHAR(60) NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    description TEXT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

-- 2) FK opcional no artigo
ALTER TABLE kb_article
    ADD COLUMN IF NOT EXISTS system_id BIGINT NULL;

-- 3) Constraint FK (comportamento: não deixa excluir sistema se tem artigo apontando)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_kb_article_system'
          AND table_name = 'kb_article'
    ) THEN
ALTER TABLE kb_article
    ADD CONSTRAINT fk_kb_article_system
        FOREIGN KEY (system_id)
            REFERENCES kb_system (id)
            ON DELETE RESTRICT;
END IF;
END $$;

-- 4) Índice para consultas por sistema
CREATE INDEX IF NOT EXISTS ix_kb_article_system_id ON kb_article(system_id);

-- 5) Seed inicial (ajuste conforme os sistemas reais da Consisa)
INSERT INTO kb_system (code, name, description)
VALUES
    ('QUINTO_EIXO', 'Quinto Eixo', 'Módulo de gestão de frotas'),
    ('SGRH', 'SGRH', 'Sistema de gestão de RH'),
    ('NOTAON', 'NotaOn', 'Emissão e gestão de notas/documentos'),
    ('BIOJOB', 'BioJob', 'Módulo relacionado ao BioJob'),
    ('CONSISANET', 'ConsisaNet', 'Portal / ecossistema Consisa')
    ON CONFLICT (code) DO NOTHING;
