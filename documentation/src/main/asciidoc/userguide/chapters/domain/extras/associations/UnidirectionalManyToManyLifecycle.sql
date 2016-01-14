INSERT INTO Person ( id )
VALUES ( 1 )

INSERT INTO Address ( number, street, id )
VALUES ( '12A', '12th Avenue', 2 )

INSERT INTO Address ( number, street, id )
VALUES ( '18B', '18th Avenue', 3 )

INSERT INTO Person ( id )
VALUES ( 4 )

INSERT INTO Person_Address ( Person_id, addresses_id )
VALUES ( 1, 2 )
INSERT INTO Person_Address ( Person_id, addresses_id )
VALUES ( 1, 3 )
INSERT INTO Person_Address ( Person_id, addresses_id )
VALUES ( 4, 2 )

DELETE FROM Person_Address
WHERE  Person_id = 1

INSERT INTO Person_Address ( Person_id, addresses_id )
VALUES ( 1, 3 )