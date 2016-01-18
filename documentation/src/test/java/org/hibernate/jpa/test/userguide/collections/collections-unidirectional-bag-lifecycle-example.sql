INSERT INTO Person ( id )
VALUES ( 1 )

INSERT INTO Phone ( number, type, id )
VALUES ( '028-234-9876', 'landline', 1 )

INSERT INTO Phone ( number, type, id )
VALUES ( '072-122-9876', 'mobile', 2 )

INSERT INTO Person_Phone ( Person_id, phones_id )
VALUES ( 1, 1 )

INSERT INTO Person_Phone ( Person_id, phones_id )
VALUES ( 1, 2 )