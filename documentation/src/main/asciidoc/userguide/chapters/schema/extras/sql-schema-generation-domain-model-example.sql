create table schema_book (
    id bigint not null,
    author varchar(100) not null,
    edition integer default 1,
    title varchar(100) not null,
    titleLength integer generated always as (char_length(title)),
    primary key (id),
    constraint uk_schema_book_title_author unique (title, author),
    constraint positive_edition check (edition > 0)
);

create index idx_schema_book_author on schema_book (author, title);
