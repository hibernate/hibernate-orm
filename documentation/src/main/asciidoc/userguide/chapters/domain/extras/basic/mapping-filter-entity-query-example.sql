SELECT
    a.id as id1_0_,
    a.active as active2_0_,
    a.amount as amount3_0_,
    a.client_id as client_i6_0_,
    a.rate as rate4_0_,
    a.account_type as account_5_0_
FROM
    Account a

-- Activate filter [activeAccount]

SELECT
    a.id as id1_0_,
    a.active as active2_0_,
    a.amount as amount3_0_,
    a.client_id as client_i6_0_,
    a.rate as rate4_0_,
    a.account_type as account_5_0_
FROM
    Account a
WHERE
    a.active = true