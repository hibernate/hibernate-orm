INSERT INTO Person ( phones, id )
VALUES ( '027-123-4567,028-234-9876', 1 )

UPDATE Person
SET    phones = '028-234-9876'
WHERE  id = 1