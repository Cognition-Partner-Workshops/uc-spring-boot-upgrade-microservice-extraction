alter table articles add column view_count integer not null default 0;

alter table article_favorites add column created_at TIMESTAMP;
