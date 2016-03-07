INSERT INTO integer_property
       ( "name", "value", id )
VALUES ( 'age', 23, 1 )

INSERT INTO string_property
       ( "name", "value", id )
VALUES ( 'name', 'John Doe', 1 )

INSERT INTO property_holder
       ( property_type, property_id, id )
VALUES ( 'S', 1, 1 )


SELECT ph.id AS id1_1_0_,
       ph.property_type AS property2_1_0_,
       ph.property_id AS property3_1_0_
FROM   property_holder ph
WHERE  ph.id = 1


SELECT sp.id AS id1_2_0_,
       sp."name" AS name2_2_0_,
       sp."value" AS value3_2_0_
FROM   string_property sp
WHERE  sp.id = 1