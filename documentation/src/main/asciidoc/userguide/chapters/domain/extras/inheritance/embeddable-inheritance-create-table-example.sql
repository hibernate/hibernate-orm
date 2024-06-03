create table TestEntity (
    id bigint not null,
    embeddable_type varchar(31) not null,
    parentProp varchar(255),
    childOneProp integer,
    subChildOneProp float(53),
    primary key (id)
)