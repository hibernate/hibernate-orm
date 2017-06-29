create table Book (
    id bigint not null,
    author varchar(255),
    ebookPublisher_name varchar(255),
    paperBackPublisher_name varchar(255),
    title varchar(255),
    ebookPublisher_country_id bigint,
    paperBackPublisher_country_id bigint,
    primary key (id)
)