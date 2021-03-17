SELECT
    c.id as id1_0_0_,
    c.latitude as latitude2_0_0_,
    c.longitude as longitud3_0_0_,
    c.name as name4_0_0_ 
FROM
    City c 
WHERE
    c.id = ?
        
-- binding parameter [1] as [BIGINT] - [1]

-- extracted value ([latitude2_0_0_] : [DOUBLE])  - [46.7712]
-- extracted value ([longitud3_0_0_] : [DOUBLE])  - [23.6236]
-- extracted value ([name4_0_0_]     : [VARCHAR]) - [Cluj]