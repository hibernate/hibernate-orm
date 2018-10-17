SELECT
    c.id as id1_1_0_,
    c.name as name2_1_0_ 
FROM
    Client c 
WHERE
    c.id = 1

SELECT
    a.id as id1_0_,
    a.active_status as active2_0_,
    a.amount as amount3_0_,
    a.client_id as client_i6_0_,
    a.rate as rate4_0_,
    a.account_type as account_5_0_
FROM
    Account a
WHERE
    a.client_id = 1