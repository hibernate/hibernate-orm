create table author (
    id bigint not null,
    first_name varchar(255),
    last_name varchar(255),
    primary key (id)
)

create table book (
    id bigint not null,
    title varchar(255),
    author_id bigint,
    primary key (id)
)

alter table book
   add constraint uk_book_title_author
   unique (title, author_id)

alter table book
   add constraint fk_book_author_id
   foreign key (author_id)
   references author