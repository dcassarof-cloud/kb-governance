create table kb_article (
                            id              bigint primary key,
                            title           text not null,
                            slug            text,
                            article_status  int not null,
                            summary         text,
                            content_html    text,
                            content_text    text,
                            revision_id     bigint,
                            reading_time    varchar(16),
                            created_date    timestamptz,
                            updated_date    timestamptz,

                            fetched_at      timestamptz not null default now(),
                            source_url      text not null,
                            source_system   varchar(30) not null default 'movidesk'
);
