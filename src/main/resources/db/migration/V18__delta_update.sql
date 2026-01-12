ALTER TABLE kb_article
    ADD COLUMN IF NOT EXISTS mov_updated_at timestamp,
    ADD COLUMN IF NOT EXISTS mov_revision_id varchar(120),
    ADD COLUMN IF NOT EXISTS last_seen_at timestamp,
    ADD COLUMN IF NOT EXISTS sync_state varchar(20);

CREATE INDEX IF NOT EXISTS idx_kb_article_mov_updated_at ON kb_article (mov_updated_at);
CREATE INDEX IF NOT EXISTS idx_kb_article_last_seen_at ON kb_article (last_seen_at);
CREATE INDEX IF NOT EXISTS idx_kb_article_mov_revision_id ON kb_article (mov_revision_id);