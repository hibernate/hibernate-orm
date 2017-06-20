create table Country (
    id bigint not null,
    name varchar(255),
    primary key (id)
)

alter table Country
    add constraint UK_p1n05aafu73sbm3ggsxqeditd
    unique (name)