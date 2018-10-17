create table Book (
    id bigint not null,
    author varchar(255),
    title varchar(255),
    primary key (id)
)

create table Book_Reader (
    book_id bigint not null,
    reader_id bigint not null
)

create table Reader (
    id bigint not null,
    name varchar(255),
    primary key (id)
)

alter table Book_Reader
    add constraint FKsscixgaa5f8lphs9bjdtpf9g
    foreign key (reader_id)
    references Reader

alter table Book_Reader
    add constraint FKoyrwu9tnwlukd1616qhck21ra
    foreign key (book_id)
    references Book

alter table Book_Reader
    add created_on timestamp
    default current_timestamp