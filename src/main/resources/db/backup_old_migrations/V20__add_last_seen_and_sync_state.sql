ALTER TABLE kb_article
    ADD COLUMN IF NOT EXISTS last_seen_at timestamptz,
    ADD COLUMN IF NOT EXISTS sync_state varchar(20);

CREATE INDEX IF NOT EXISTS idx_kb_article_last_seen_at ON kb_article(last_seen_at);
CREATE INDEX IF NOT EXISTS idx_kb_article_sync_state ON kb_article(sync_state);
