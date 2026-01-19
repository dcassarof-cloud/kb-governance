create table kb_sync_issue (
                               id bigserial primary key,

    -- artigo do Movidesk (mesmo se não existir no kb_article)
                               article_id bigint not null,

    -- tipos previstos (você pode evoluir depois)
                               issue_type varchar(40) not null, -- NOT_FOUND, EMPTY_CONTENT, MENU_NULL, ERROR

    -- mensagem curta e útil (não logar HTML inteiro)
                               message varchar(400),

                               created_at timestamp not null default now(),

    -- opcional (recomendado): permitir “resolver”
                               is_resolved boolean not null default false,
                               resolved_at timestamp null
);

create index idx_kb_sync_issue_article on kb_sync_issue(article_id);
create index idx_kb_sync_issue_type on kb_sync_issue(issue_type);
create index idx_kb_sync_issue_open on kb_sync_issue(is_resolved) where is_resolved = false;

-- evita spam: não criar 100 issues iguais abertas pro mesmo artigo
create unique index uq_kb_sync_issue_open_unique
    on kb_sync_issue(article_id, issue_type)
    where is_resolved = false;
