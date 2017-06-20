create table person (
    id bigint not null,
    primary key (id)
)

create table call_register (
    person_id bigint not null,
    call_register integer,
    country_code varchar(255) not null,
    operator_code varchar(255) not null,
    subscriber_code varchar(255) not null,
    primary key (person_id, country_code, operator_code, subscriber_code)
)

alter table call_register
    add constraint FKqyj2at6ik010jqckeaw23jtv2
    foreign key (person_id)
    references person