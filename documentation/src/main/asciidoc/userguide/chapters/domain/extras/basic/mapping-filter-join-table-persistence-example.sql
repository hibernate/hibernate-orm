INSERT INTO Client (name, id)
VALUES ('John Doe', 1)

INSERT INTO Account (active, amount, client_id, rate, account_type, id)
VALUES (true, 5000.0, 1, 0.0125, 'CREDIT', 1)

INSERT INTO Account (active, amount, client_id, rate, account_type, id)
VALUES (false, 0.0, 1, 0.0105, 'DEBIT', 2)

INSERT INTO Account (active, amount, client_id, rate, account_type, id)
VALUES (true, 250.0, 1, 0.0105, 'DEBIT', 3)

INSERT INTO Client_Account (Client_id, order_id, accounts_id)
VALUES (1, 0, 1)

INSERT INTO Client_Account (Client_id, order_id, accounts_id)
VALUES (1, 0, 1)

INSERT INTO Client_Account (Client_id, order_id, accounts_id)
VALUES (1, 1, 2)

INSERT INTO Client_Account (Client_id, order_id, accounts_id)
VALUES (1, 2, 3)