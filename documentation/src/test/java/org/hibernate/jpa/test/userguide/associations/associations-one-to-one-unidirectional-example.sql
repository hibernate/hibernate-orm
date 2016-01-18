CREATE TABLE Phone (
    id BIGINT NOT NULL ,
    number VARCHAR(255) ,
    details_id BIGINT ,
    PRIMARY KEY ( id )
)

CREATE TABLE PhoneDetails (
    id BIGINT NOT NULL ,
    provider VARCHAR(255) ,
    technology VARCHAR(255) ,
    PRIMARY KEY ( id )
)

ALTER TABLE Phone
ADD CONSTRAINT FKnoj7cj83ppfqbnvqqa5kolub7
FOREIGN KEY (details_id) REFERENCES PhoneDetails