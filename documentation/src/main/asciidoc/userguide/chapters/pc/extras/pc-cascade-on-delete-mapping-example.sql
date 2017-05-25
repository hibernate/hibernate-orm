create table Person (
    id bigint not null,
    name varchar(255),
    primary key (id)
)

create table Phone (
    id bigint not null,
    "number" varchar(255),
    owner_id bigint,
    primary key (id)
)

alter table Phone
    add constraint FK82m836qc1ss2niru7eogfndhl
    foreign key (owner_id)
    references Person
    on delete cascade