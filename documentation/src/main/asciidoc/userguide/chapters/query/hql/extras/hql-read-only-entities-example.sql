SELECT c.id AS id1_5_ ,
       c.duration AS duration2_5_ ,
       c.phone_id AS phone_id4_5_ ,
       c.call_timestamp AS call_tim3_5_
FROM   phone_call c
INNER JOIN phone p ON c.phone_id = p.id
WHERE   p.phone_number = '123-456-7890'