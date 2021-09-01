create table Book (
    id bigint not null,
    author varchar(255),
    ebook_pub_name varchar(255),
    paper_back_pub_name varchar(255),
    title varchar(255),
    ebook_pub_country_id bigint,
    paper_back_pub_country_id bigint,
    primary key (id)
)

alter table Book
    add constraint FKm39ibh5jstybnslaoojkbac2g
    foreign key (ebook_pub_country_id)
    references Country

alter table Book
    add constraint FK7kqy9da323p7jw7wvqgs6aek7
    foreign key (paper_back_pub_country_id)
    references Country