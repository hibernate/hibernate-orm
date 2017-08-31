select
    queryaudit0_.id as id1_3_,
    queryaudit0_.REV as REV2_3_,
    queryaudit0_.REVTYPE as REVTYPE3_3_,
    queryaudit0_.REVEND as REVEND4_3_,
    queryaudit0_.created_on as created_5_3_,
    queryaudit0_.createdOn_MOD as createdO6_3_,
    queryaudit0_.firstName as firstNam7_3_,
    queryaudit0_.firstName_MOD as firstNam8_3_,
    queryaudit0_.lastName as lastName9_3_,
    queryaudit0_.lastName_MOD as lastNam10_3_,
    queryaudit0_.address_id as address11_3_,
    queryaudit0_.address_MOD as address12_3_
from
    Customer_AUD queryaudit0_
where
    queryaudit0_.REV=?
    and queryaudit0_.id=?
    and queryaudit0_.lastName_MOD=?
    and queryaudit0_.firstName_MOD=?

-- binding parameter [1] as [INTEGER] - [2]
-- binding parameter [2] as [BIGINT]  - [1]
-- binding parameter [3] as [BOOLEAN] - [true]
-- binding parameter [4] as [BOOLEAN] - [false]