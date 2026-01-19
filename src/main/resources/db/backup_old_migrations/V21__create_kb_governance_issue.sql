create table if not exists kb_governance_issue (
                                                   id bigserial primary key,

                                                   article_id bigint not null references kb_article(id),

    issue_type varchar(50) not null,          -- INCOMPLETE, DUPLICATE, etc
    status varchar(20) not null default 'OPEN', -- OPEN, RESOLVED

    severity varchar(10) not null default 'WARN', -- INFO, WARN, ERROR
    message varchar(400) null,

    evidence jsonb null,                      -- provas (ex: chars, hash, ids duplicados)
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    resolved_at timestamptz null,
    resolved_by varchar(100) null
    );

create index if not exists ix_kb_gov_issue_article on kb_governance_issue(article_id);
create index if not exists ix_kb_gov_issue_type_status on kb_governance_issue(issue_type, status);
