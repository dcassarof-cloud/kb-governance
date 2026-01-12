
create table if not exists kb_sync_run (
                                           id bigserial primary key,

    -- quando começou/terminou
                                           started_at timestamptz not null,
                                           finished_at timestamptz null,
                                           duration_ms bigint null,

    -- modo e status
                                           mode varchar(30) not null,         -- FULL / DELTA_WINDOW
    status varchar(30) not null,       -- SUCCESS / FAILED / RUNNING

-- parâmetros usados
    days_back int null,
    page_size int null,

    -- contadores do resultado
    total_found int null,
    synced_count int null,
    updated_count int null,
    not_found_count int null,
    error_count int null,
    menu_null_count int null,
    empty_content_count int null,
    menu_not_mapped_count int null,
    geral_count int null,

    -- detalhe resumido (erro, log)
    message varchar(800) null,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
    );

create index if not exists ix_kb_sync_run_started_at on kb_sync_run (started_at desc);
create index if not exists ix_kb_sync_run_status on kb_sync_run (status);
