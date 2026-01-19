-- 1) adiciona colunas faltantes (seguro mesmo se já existirem)
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS note varchar(400);

ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS synced_count     integer NOT NULL DEFAULT 0;
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS updated_count    integer NOT NULL DEFAULT 0;
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS skipped_count    integer NOT NULL DEFAULT 0;
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS not_found_count  integer NOT NULL DEFAULT 0;
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS error_count      integer NOT NULL DEFAULT 0;

ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS started_at   timestamptz;
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS finished_at  timestamptz;
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS duration_ms  bigint;
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS mode         varchar(30);
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS days_back    integer;
ALTER TABLE kb_sync_run ADD COLUMN IF NOT EXISTS status       varchar(20);

-- 2) garante NOT NULL onde a entidade exige (sem quebrar dados antigos)
UPDATE kb_sync_run
SET
    synced_count    = COALESCE(synced_count, 0),
    updated_count   = COALESCE(updated_count, 0),
    skipped_count   = COALESCE(skipped_count, 0),
    not_found_count = COALESCE(not_found_count, 0),
    error_count     = COALESCE(error_count, 0);

ALTER TABLE kb_sync_run ALTER COLUMN synced_count    SET NOT NULL;
ALTER TABLE kb_sync_run ALTER COLUMN updated_count   SET NOT NULL;
ALTER TABLE kb_sync_run ALTER COLUMN skipped_count   SET NOT NULL;
ALTER TABLE kb_sync_run ALTER COLUMN not_found_count SET NOT NULL;
ALTER TABLE kb_sync_run ALTER COLUMN error_count     SET NOT NULL;

-- 3) índices (seguro)
CREATE INDEX IF NOT EXISTS ix_kb_sync_run_started_at ON kb_sync_run(started_at);
CREATE INDEX IF NOT EXISTS ix_kb_sync_run_status     ON kb_sync_run(status);
