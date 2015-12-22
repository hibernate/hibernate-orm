CREATE TABLE Person (
    id BIGINT NOT NULL ,
    PRIMARY KEY ( id )
)

CREATE TABLE Phone (
    id BIGINT NOT NULL ,
    number VARCHAR(255) ,
    since TIMESTAMP ,
    type INTEGER ,
    PRIMARY KEY ( id )
)

CREATE TABLE phone_register (
    phone_id BIGINT NOT NULL ,
    person_id BIGINT NOT NULL ,
    PRIMARY KEY ( phone_id, person_id )
)

ALTER TABLE phone_register
ADD CONSTRAINT FKc3jajlx41lw6clbygbw8wm65w
FOREIGN KEY (person_id) REFERENCES Phone

ALTER TABLE phone_register
ADD CONSTRAINT FK6npoomh1rp660o1b55py9ndw4
FOREIGN KEY (phone_id) REFERENCES Person