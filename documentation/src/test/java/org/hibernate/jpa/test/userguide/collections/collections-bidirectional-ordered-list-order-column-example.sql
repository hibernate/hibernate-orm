CREATE TABLE Phone (
    id BIGINT NOT NULL ,
    number VARCHAR(255) ,
    type VARCHAR(255) ,
    person_id BIGINT ,
    order_id INTEGER ,
    PRIMARY KEY ( id )
)