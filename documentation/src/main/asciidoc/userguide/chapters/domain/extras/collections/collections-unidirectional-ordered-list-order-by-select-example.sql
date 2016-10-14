SELECT
   phones0_.Person_id AS Person_i1_1_0_,
   phones0_.phones_id AS phones_i2_1_0_,
   unidirecti1_.id AS id1_2_1_,
   unidirecti1_."number" AS number2_2_1_,
   unidirecti1_.type AS type3_2_1_ 
FROM
   Person_Phone phones0_ 
INNER JOIN
   Phone unidirecti1_ ON phones0_.phones_id=unidirecti1_.id
WHERE
   phones0_.Person_id = 1
ORDER BY
   unidirecti1_."number"