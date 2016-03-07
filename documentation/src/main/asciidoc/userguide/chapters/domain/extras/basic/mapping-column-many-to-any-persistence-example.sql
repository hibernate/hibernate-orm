INSERT INTO integer_property
       ( "name", "value", id )
VALUES ( 'age', 23, 1 )

INSERT INTO string_property
       ( "name", "value", id )
VALUES ( 'name', 'John Doe', 1 )

INSERT INTO property_repository ( id )
VALUES ( 1 )

INSERT INTO repository_properties
    ( repository_id , property_type , property_id )
VALUES
    ( 1 , 'I' , 1 )

INSERT INTO repository_properties
    ( repository_id , property_type , property_id )
VALUES
    ( 1 , 'S' , 1 )

SELECT pr.id AS id1_1_0_
FROM   property_repository pr
WHERE  pr.id = 1

SELECT ip.id AS id1_0_0_ ,
       integerpro0_."name" AS name2_0_0_ ,
       integerpro0_."value" AS value3_0_0_
FROM   integer_property integerpro0_
WHERE  integerpro0_.id = 1

SELECT sp.id AS id1_3_0_ ,
       sp."name" AS name2_3_0_ ,
       sp."value" AS value3_3_0_
FROM   string_property sp
WHERE  sp.id = 1
