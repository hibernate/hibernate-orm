select
    c.id as id1_3_0_,
    c.REV as REV2_3_0_,
    defaultrev1_.REV as REV1_4_1_,
    c.REVTYPE as REVTYPE3_3_0_,
    c.REVEND as REVEND4_3_0_,
    c.created_on as created_5_3_0_,
    c.createdOn_MOD as createdO6_3_0_,
    c.firstName as firstNam7_3_0_,
    c.firstName_MOD as firstNam8_3_0_,
    c.lastName as lastName9_3_0_,
    c.lastName_MOD as lastNam10_3_0_,
    c.address_id as address11_3_0_,
    c.address_MOD as address12_3_0_,
    defaultrev1_.REVTSTMP as REVTSTMP2_4_1_
from
    Customer_AUD c cross
join
    REVINFO defaultrev1_
where
    c.id = ?
    and c.lastName_MOD = ?
    and c.REV=defaultrev1_.REV
order by
    c.REV asc

-- binding parameter [1] as [BIGINT]  - [1]
-- binding parameter [2] as [BOOLEAN] - [true]