create table if not exists kb_sync_run (
                                           id bigserial primary key,

                                           started_at timestamptz not null,
                                           finished_at timestamptz null,
                                           duration_ms bigint null,

                                           mode varchar(30) not null,         -- enum SyncMode como STRING
    days_back int null,

    status varchar(20) not null,       -- enum SyncRunStatus como STRING

    synced_count int not null default 0,
    updated_count int not null default 0,
    skipped_count int not null default 0,
    not_found_count int not null default 0,
    error_count int not null default 0,

    note varchar(400) null
    );

create index if not exists ix_kb_sync_run_started_at on kb_sync_run (started_at desc);
create index if not exists ix_kb_sync_run_status on kb_sync_run (status);
create index if not exists ix_kb_sync_run_mode on kb_sync_run (mode);
