create table person (
    id int8 not null,
    primary key (id)
)

create table call_register (
    person_id int8 not null,
    phone_number int4,
    call_timestamp_epoch int8 not null,
    primary key (person_id, call_timestamp_epoch)
)

alter table if exists call_register
    add constraint FKsn58spsregnjyn8xt61qkxsub
    foreign key (person_id)
    references person
