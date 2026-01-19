-- =========================================================
-- GOVERNANÇA KB - ESTRUTURA BASE
-- =========================================================

-- 1) Campos novos na tabela kb_article
alter table kb_article
    add column source_menu_id bigint,
  add column content_hash varchar(64),
  add column governance_status varchar(30) not null default 'NEW',
  add column approved_revision_id bigint,
  add column approved_at timestamptz;

-- 2) Índices importantes (performance)
create index if not exists idx_kb_article_content_hash
    on kb_article(content_hash);

create index if not exists idx_kb_article_source_menu_id
    on kb_article(source_menu_id);

create index if not exists idx_kb_article_governance_status
    on kb_article(governance_status);

-- 3) Tabela de mapeamento Menu Movidesk → Sistema Consisa
create table if not exists kb_menu_map (
                                           id bigserial primary key,

                                           source_system varchar(40) not null default 'movidesk',
    source_menu_id bigint not null,
    source_menu_name varchar(200),

    system_id bigint not null
    references kb_system(id),

    is_active boolean not null default true,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    unique (source_system, source_menu_id)
    );

create index if not exists idx_kb_menu_map_system
    on kb_menu_map(system_id);
