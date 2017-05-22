INSERT INTO Phone("number", person_id, type, id)
VALUES ('028-234-9876', 1, 'landline', 1)

INSERT INTO Phone("number", person_id, type, id)
VALUES ('072-122-9876', 1, 'mobile', 2)

UPDATE Phone
SET order_id = 100
WHERE id = 1

UPDATE Phone
SET order_id = 101
WHERE id = 2