create table public.book (
  id bigint not null,
  author varchar(255),
  title varchar(255),
  primary key (id)
) engine=InnoDB