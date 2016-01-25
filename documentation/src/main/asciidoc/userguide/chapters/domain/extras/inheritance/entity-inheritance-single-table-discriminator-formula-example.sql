CREATE TABLE Account (
    id int8 NOT NULL ,
    balance NUMERIC(19, 2) ,
    interestRate NUMERIC(19, 2) ,
    owner VARCHAR(255) ,
    debitKey VARCHAR(255) ,
    overdraftFee NUMERIC(19, 2) ,
    creditKey VARCHAR(255) ,
    creditLimit NUMERIC(19, 2) ,
    PRIMARY KEY ( id )
)