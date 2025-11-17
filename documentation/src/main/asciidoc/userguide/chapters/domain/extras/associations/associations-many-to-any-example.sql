CREATE TABLE loans (
    id BIGINT NOT NULL,
    ...,
    PRIMARY KEY ( id )
)

CREATE TABLE loan_payments (
    loan_fk BIGINT NOT NULL,
    payment_type VARCHAR(255),
    payment_fk BIGINT NOT NULL
)