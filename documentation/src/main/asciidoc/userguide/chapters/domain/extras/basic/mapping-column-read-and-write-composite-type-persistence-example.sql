INSERT INTO Savings (money, currency, id)
VALUES (10 * 100, 'USD', 1)

SELECT
    s.id as id1_0_0_,
    s.money / 100 as money2_0_0_,
    s.currency as currency3_0_0_
FROM
    Savings s
WHERE
    s.id = 1