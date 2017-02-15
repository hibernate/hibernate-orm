SELECT
    a.id as id1_0_0_,
    a.active as active2_0_0_,
    a.amount as amount3_0_0_,
    a.client_id as client_i6_0_0_,
    a.rate as rate4_0_0_,
    a.account_type as account_5_0_0_,
    c.id as id1_1_1_,
    c.name as name2_1_1_ 
FROM
    Account a 
LEFT OUTER JOIN
    Client c 
        ON a.client_id=c.id
WHERE
    a.id = 2