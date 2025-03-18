create table Customer_AUD (
    id bigint not null,
    REV integer not null,
    REVTYPE tinyint,
    created_on timestamp,
    createdOn_MOD boolean,
    firstName varchar(255),
    firstName_MOD boolean,
    lastName varchar(255),
    lastName_MOD boolean,
    primary key (id, REV)
)