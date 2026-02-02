-- =====================================================
-- KB GOVERNANCE - MIGRATION V10
-- Ticket status tracking for assignments
-- =====================================================

ALTER TABLE kb_article_assignment
    ADD COLUMN ticket_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN ticket_last_error TEXT,
    ADD COLUMN ticket_created_at TIMESTAMPTZ,
    ADD COLUMN ticket_retry_count INT NOT NULL DEFAULT 0;

UPDATE kb_article_assignment
SET ticket_status = 'CREATED',
    ticket_created_at = COALESCE(ticket_created_at, updated_at)
WHERE ticket_id IS NOT NULL;
