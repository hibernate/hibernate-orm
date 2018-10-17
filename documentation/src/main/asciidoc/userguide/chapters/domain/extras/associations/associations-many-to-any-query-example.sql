SELECT pr.id AS id1_1_0_
FROM   property_repository pr
WHERE  pr.id = 1

SELECT ip.id AS id1_0_0_ ,
       ip."name" AS name2_0_0_ ,
       ip."value" AS value3_0_0_
FROM   integer_property ip
WHERE  ip.id = 1

SELECT sp.id AS id1_3_0_ ,
       sp."name" AS name2_3_0_ ,
       sp."value" AS value3_3_0_
FROM   string_property sp
WHERE  sp.id = 1
