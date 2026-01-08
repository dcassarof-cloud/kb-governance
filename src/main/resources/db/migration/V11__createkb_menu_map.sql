-- V11__createkb_menu_map.sql
-- Padronização e garantia de estrutura da kb_menu_map
-- Alinhada ao schema real já existente

create table if not exists kb_menu_map (
                                           id bigserial primary key,

                                           source_system varchar(30) not null,
    source_menu_id bigint not null,
    source_menu_name varchar(255) not null,

    system_id bigint not null references kb_system(id),

    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
    );

-- índices
create index if not exists idx_kb_menu_map_system
    on kb_menu_map(system_id);

create index if not exists idx_kb_menu_map_source_menu
    on kb_menu_map(source_system, source_menu_id);

-- unique: um menu ativo por sistema de origem
create unique index if not exists uq_kb_menu_map_active
    on kb_menu_map(source_system, source_menu_id)
    where is_active = true;
