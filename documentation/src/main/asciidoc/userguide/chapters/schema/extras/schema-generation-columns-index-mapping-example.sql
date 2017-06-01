create table author (
    id bigint not null,
    first_name varchar(255),
    last_name varchar(255),
    primary key (id)
)

create index idx_author_first_last_name
    on author (first_name, last_name)