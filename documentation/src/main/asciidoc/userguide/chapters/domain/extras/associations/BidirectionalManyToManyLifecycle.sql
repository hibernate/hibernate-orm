INSERT INTO Person ( registrationNumber, id )
VALUES ( 'ABC-123', 1 )

INSERT INTO Address ( number, postalCode, street, id )
VALUES ( '12A', '4005A', '12th Avenue', 2 )

INSERT INTO Address ( number, postalCode, street, id )
VALUES ( '18B', '4007B', '18th Avenue', 3 )

INSERT INTO Person ( registrationNumber, id )
VALUES ( 'DEF-456', 4 )

INSERT INTO Person_Address ( owners_id, addresses_id )
VALUES ( 1, 2 )

INSERT INTO Person_Address ( owners_id, addresses_id )
VALUES ( 1, 3 )

INSERT INTO Person_Address ( owners_id, addresses_id )
VALUES ( 4, 2 )

DELETE FROM Person_Address
WHERE  owners_id = 1

INSERT INTO Person_Address ( owners_id, addresses_id )
VALUES ( 1, 3 )