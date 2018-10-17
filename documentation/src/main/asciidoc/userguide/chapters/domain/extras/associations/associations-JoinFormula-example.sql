CREATE TABLE countries (
    id int4 NOT NULL,
    name VARCHAR(255),
    PRIMARY KEY ( id )
)

CREATE TABLE users (
    id int8 NOT NULL,
    firstName VARCHAR(255),
    lastName VARCHAR(255),
    phoneNumber VARCHAR(255),
    PRIMARY KEY ( id )
)