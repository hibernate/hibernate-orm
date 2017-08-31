select
    queryaudit0_.id as id1_3_0_,
    queryaudit0_.REV as REV2_3_0_,
    defaultrev1_.REV as REV1_4_1_,
    queryaudit0_.REVTYPE as REVTYPE3_3_0_,
    queryaudit0_.REVEND as REVEND4_3_0_,
    queryaudit0_.created_on as created_5_3_0_,
    queryaudit0_.createdOn_MOD as createdO6_3_0_,
    queryaudit0_.firstName as firstNam7_3_0_,
    queryaudit0_.firstName_MOD as firstNam8_3_0_,
    queryaudit0_.lastName as lastName9_3_0_,
    queryaudit0_.lastName_MOD as lastNam10_3_0_,
    queryaudit0_.address_id as address11_3_0_,
    queryaudit0_.address_MOD as address12_3_0_,
    defaultrev1_.REVTSTMP as REVTSTMP2_4_1_
from
    Customer_AUD queryaudit0_ cross
join
    REVINFO defaultrev1_
where
    queryaudit0_.id = ?
    and queryaudit0_.lastName_MOD = ?
    and queryaudit0_.REV=defaultrev1_.REV
order by
    queryaudit0_.REV asc

-- binding parameter [1] as [BIGINT]  - [1]
-- binding parameter [2] as [BOOLEAN] - [true]