create table Customer (
    id integer not null,
    accountsPayableXrefId binary,
    image blob,
    name varchar(255),
    primary key (id)
)

create table Book (
    id bigint not null,
    isbn varchar(255),
    title varchar(255),
    author_id bigint,
    primary key (id)
)

create table Person (
    id bigint not null,
    name varchar(255),
    primary key (id)
)

alter table Book
    add constraint UK_u31e1frmjp9mxf8k8tmp990i unique (isbn)

alter table Book
    add constraint FKrxrgiajod1le3gii8whx2doie
    foreign key (author_id)
    references Person