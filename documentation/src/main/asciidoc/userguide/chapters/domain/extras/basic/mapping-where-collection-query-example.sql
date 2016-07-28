SELECT
    c.client_id as client_i6_0_0_,
    c.id as id1_0_0_,
    c.id as id1_0_1_,
    c.active as active2_0_1_,
    c.amount as amount3_0_1_,
    c.client_id as client_i6_0_1_,
    c.rate as rate4_0_1_,
    c.account_type as account_5_0_1_
FROM
    Account c
WHERE ( c.active = true and c.account_type = 'CREDIT' ) AND c.client_id = 1

SELECT
    d.client_id as client_i6_0_0_,
    d.id as id1_0_0_,
    d.id as id1_0_1_,
    d.active as active2_0_1_,
    d.amount as amount3_0_1_,
    d.client_id as client_i6_0_1_,
    d.rate as rate4_0_1_,
    d.account_type as account_5_0_1_
FROM
    Account d
WHERE ( d.active = true and d.account_type = 'DEBIT' ) AND d.client_id = 1