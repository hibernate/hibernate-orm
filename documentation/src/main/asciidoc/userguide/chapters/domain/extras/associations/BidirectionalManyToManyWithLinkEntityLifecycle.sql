INSERT  INTO Person ( registrationNumber, id )
VALUES  ( 'ABC-123', 1 )

INSERT  INTO Person ( registrationNumber, id )
VALUES  ( 'DEF-456', 2 )

INSERT  INTO Address ( number, postalCode, street, id )
VALUES  ( '12A', '4005A', '12th Avenue', 3 )

INSERT  INTO Address ( number, postalCode, street, id )
VALUES  ( '18B', '4007B', '18th Avenue', 4 )

INSERT  INTO PersonAddress ( person_id, address_id )
VALUES  ( 1, 3 )

INSERT  INTO PersonAddress ( person_id, address_id )
VALUES  ( 1, 4 )

INSERT  INTO PersonAddress ( person_id, address_id )
VALUES  ( 2, 3 )

DELETE  FROM PersonAddress
WHERE   person_id = 1 AND address_id = 3