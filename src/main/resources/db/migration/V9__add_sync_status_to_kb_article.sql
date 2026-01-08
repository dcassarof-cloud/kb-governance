alter table kb_article
    add column if not exists sync_status varchar(30);

alter table kb_article
    add column if not exists sync_error_message varchar(400);
