select
    c.id as id1_3_,
    c.REV as REV2_3_,
    c.REVTYPE as REVTYPE3_3_,
    c.REVEND as REVEND4_3_,
    c.created_on as created_5_3_,
    c.createdOn_MOD as createdO6_3_,
    c.firstName as firstNam7_3_,
    c.firstName_MOD as firstNam8_3_,
    c.lastName as lastName9_3_,
    c.lastName_MOD as lastNam10_3_,
    c.address_id as address11_3_,
    c.address_MOD as address12_3_
from
    Customer_AUD c
where
    c.REV=?
    and c.id=?
    and c.lastName_MOD=?
    and c.firstName_MOD=?

-- binding parameter [1] as [INTEGER] - [2]
-- binding parameter [2] as [BIGINT]  - [1]
-- binding parameter [3] as [BOOLEAN] - [true]
-- binding parameter [4] as [BOOLEAN] - [false]