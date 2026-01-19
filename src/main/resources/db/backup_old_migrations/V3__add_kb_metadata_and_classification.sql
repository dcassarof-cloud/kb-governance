ALTER TABLE kb_article
    ADD COLUMN IF NOT EXISTS kb_menu TEXT,
    ADD COLUMN IF NOT EXISTS kb_category TEXT,
    ADD COLUMN IF NOT EXISTS kb_breadcrumb TEXT,
    ADD COLUMN IF NOT EXISTS kb_tags TEXT,
    ADD COLUMN IF NOT EXISTS classification_method VARCHAR(30),
    ADD COLUMN IF NOT EXISTS needs_review BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS ix_kb_article_needs_review ON kb_article(needs_review);
