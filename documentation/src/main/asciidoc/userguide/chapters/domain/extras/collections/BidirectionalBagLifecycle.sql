INSERT INTO Phone (number, person_id, type, id) 
VALUES ( '028-234-9876', 1, 'landline', 1 )

INSERT INTO Phone (number, person_id, type, id) 
VALUES ( '072-122-9876', 1, 'mobile', 2 )

UPDATE Phone
SET person_id = NULL, type = 'landline' where id = 1