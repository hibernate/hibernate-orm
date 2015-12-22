select
   phones0_.Person_id as Person_i1_1_0_,
   phones0_.phones_id as phones_i2_1_0_,
   phones0_.order_id as order_id3_0_,
   unidirecti1_.id as id1_2_1_,
   unidirecti1_.number as number2_2_1_,
   unidirecti1_.type as type3_2_1_
from
   Person_Phone phones0_
inner join
   Phone unidirecti1_
      on phones0_.phones_id=unidirecti1_.id
where
   phones0_.Person_id = 1