create table hibernate_sequences (
    sequence_name varchar2(255 char) not null,
    next_val number(19,0),
    primary key (sequence_name)
)