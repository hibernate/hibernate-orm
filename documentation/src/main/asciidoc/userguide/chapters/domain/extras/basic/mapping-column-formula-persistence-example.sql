INSERT INTO Account (credit, rate, id)
VALUES (5000.0, 0.0125, 1)

SELECT
    a.id as id1_0_0_,
    a.credit as credit2_0_0_,
    a.rate as rate3_0_0_,
    a.credit * a.rate as formula0_0_
FROM
    Account a
WHERE
    a.id = 1