ALTER TABLE kb_article
    ADD COLUMN IF NOT EXISTS last_seen_at timestamptz,
    ADD COLUMN IF NOT EXISTS sync_state varchar(20);
