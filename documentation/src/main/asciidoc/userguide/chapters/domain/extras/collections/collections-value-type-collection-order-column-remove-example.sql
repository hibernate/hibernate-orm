DELETE FROM Person_phones
WHERE  Person_id = 1
       AND order_id = 1

UPDATE Person_phones
SET    phones = '456-000-1234'
WHERE  Person_id = 1
       AND order_id = 0