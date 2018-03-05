INSERT INTO Person
       ( id )
VALUES ( 1 )

INSERT INTO Phone
       ( "number", person_id, id )
VALUES ( '123-456-7890', 1, 2 )

INSERT INTO Phone
       ( "number", person_id, id )
VALUES ( '321-654-0987', 1, 3 )

DELETE FROM Phone
WHERE  id = 2