select
    c.id as id1_1_,
    c.REV as REV2_1_,
    c.REVTYPE as REVTYPE3_1_,
    c.REVEND as REVEND4_1_,
    c.created_on as created_5_1_,
    c.firstName as firstNam6_1_,
    c.lastName as lastName7_1_
from
    Customer_AUD c
where
    c.REV <= ?
    and c.REVTYPE <> ?
    and (
        c.REVEND > ?
        or c.REVEND is null
    )
        
-- binding parameter [1] as [INTEGER] - [1]
-- binding parameter [2] as [INTEGER] - [2]
-- binding parameter [3] as [INTEGER] - [1]