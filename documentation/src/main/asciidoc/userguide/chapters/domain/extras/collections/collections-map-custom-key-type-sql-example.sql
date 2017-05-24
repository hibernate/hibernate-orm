create table person (
    id int8 not null,
    primary key (id)
)

create table call_register (
    phone_id int8 not null,
    phone_number int4,
    call_timestamp_epoch int8 not null,
    primary key (phone_id, call_key)
)

alter table if exists call_register
    add constraint FKsn58spsregnjyn8xt61qkxsub
    foreign key (phone_id)
    references person

alter table if exists call_register
    add constraint FKsn58spsregnjyn8xt61qkxsub
    foreign key (phone_id)
    references person