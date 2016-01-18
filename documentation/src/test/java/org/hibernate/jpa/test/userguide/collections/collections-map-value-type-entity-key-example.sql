CREATE TABLE Person (
    id BIGINT NOT NULL ,
    PRIMARY KEY ( id )
)

CREATE TABLE phone_register (
    Person_id BIGINT NOT NULL ,
    since TIMESTAMP ,
    number VARCHAR(255) NOT NULL ,
    type INTEGER NOT NULL ,
    PRIMARY KEY ( Person_id, number, type )
)

ALTER TABLE phone_register
ADD CONSTRAINT FKrmcsa34hr68of2rq8qf526mlk
FOREIGN KEY (Person_id) REFERENCES Person