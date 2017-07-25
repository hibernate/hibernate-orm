select
    c.id as id1_1_,
    c.REV as REV2_1_,
    c.REVTYPE as REVTYPE3_1_,
    c.created_on as created_4_1_,
    c.firstName as firstNam5_1_,
    c.lastName as lastName6_1_
from
    Customer_AUD c
where
    c.REV = (
        select
            max( c_max.REV )
        from
            Customer_AUD c_max
        where
            c_max.REV <= ?
            and c.id = c_max.id
    )
    and c.REVTYPE <> ?

-- binding parameter [1] as [INTEGER] - [1]
-- binding parameter [2] as [INTEGER] - [2]