INSERT INTO Account (balance, interestRate, owner, overdraftFee, DTYPE, id)
VALUES (100, 1.5, 'John Doe', 25, 'Debit', 1)

INSERT INTO Account (balance, interestRate, owner, overdraftFee, DTYPE, id)
VALUES (1000, 1.9, 'John Doe', 5000, 'Credit', 2)

INSERT INTO Account (balance, interestRate, owner, id)
VALUES (1000, 1.9, 'John Doe', 3)

INSERT INTO Account (DTYPE, active, balance, interestRate, owner, id)
VALUES ('Other', true, 25, 0.5, 'Vlad', 4)

SELECT a.id as id2_0_,
       a.balance as balance3_0_,
       a.interestRate as interest4_0_,
       a.owner as owner5_0_,
       a.overdraftFee as overdraf6_0_,
       a.creditLimit as creditLi7_0_,
       a.active as active8_0_,
       a.DTYPE as DTYPE1_0_ 
FROM   Account a
