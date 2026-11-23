/*
    Needed because the test model maps a "non standard" schema.
    The test models `User#detail` and `UserDetail#user` as a
    bidirectional one-to-one, meaning that Hibernate would
    normally (and correctly) create `t_user_details.user_fk` as
    unique.  The test model changes that cardinality by use of
    `@Where`.
*/
drop table if exists t_user_details cascade;
drop table if exists t_user_skills cascade;
drop table if exists t_users cascade;
create table t_user_details (
    detail_id integer not null,
    is_active boolean,
    city varchar(255),
    user_fk integer,
    primary key (detail_id)
);
create table t_user_skills (
    skill_id integer not null,
    has_deleted boolean,
    skill_name varchar(255),
    user_fk integer,
    primary key (skill_id)
);
create table t_users (
	user_id integer not null,
	user_name varchar(255),
	primary key (user_id)
);
