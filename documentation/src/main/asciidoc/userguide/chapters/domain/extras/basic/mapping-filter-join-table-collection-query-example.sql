SELECT
    ca.Client_id as Client_i1_2_0_,
    ca.accounts_id as accounts2_2_0_,
    ca.order_id as order_id3_0_,
    a.id as id1_0_1_,
    a.amount as amount3_0_1_,
    a.rate as rate4_0_1_,
    a.account_type as account_5_0_1_
FROM
    Client_Account ca
INNER JOIN
    Account a
ON  ca.accounts_id=a.id
WHERE
    ca.order_id <= ?
    AND ca.Client_id = ?

-- binding parameter [1] as [INTEGER] - [1]
-- binding parameter [2] as [BIGINT] - [1]