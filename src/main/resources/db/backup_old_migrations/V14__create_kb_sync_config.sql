create table if not exists kb_sync_config (
                                              id bigint primary key,
                                              enabled boolean not null default true,
                                              mode varchar(30) not null default 'DELTA_WINDOW',
    interval_minutes int not null default 60,
    days_back int not null default 2,
    last_started_at timestamptz null,
    last_finished_at timestamptz null,
    updated_at timestamptz not null default now()
    );

-- garante “singleton”
insert into kb_sync_config (id)
values (1)
    on conflict (id) do nothing;
