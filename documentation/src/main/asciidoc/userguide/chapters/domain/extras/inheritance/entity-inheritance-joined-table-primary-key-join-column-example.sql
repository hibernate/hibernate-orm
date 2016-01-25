CREATE TABLE CreditAccount (
    creditLimit NUMERIC(19, 2) ,
    account_id BIGINT NOT NULL ,
    PRIMARY KEY ( account_id )
)

CREATE TABLE DebitAccount (
    overdraftFee NUMERIC(19, 2) ,
    account_id BIGINT NOT NULL ,
    PRIMARY KEY ( account_id )
)

ALTER TABLE CreditAccount
ADD CONSTRAINT FK8ulmk1wgs5x7igo370jt0q005
FOREIGN KEY (account_id) REFERENCES Account

ALTER TABLE DebitAccount
ADD CONSTRAINT FK7wjufa570onoidv4omkkru06j
FOREIGN KEY (account_id) REFERENCES Account