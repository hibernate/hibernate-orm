select
    c.id as id1_3_,
    c.REV as REV2_3_,
    c.REVTYPE as REVTYPE3_3_,
    c.REVEND as REVEND4_3_,
    c.created_on as created_5_3_,
    c.firstName as firstNam6_3_,
    c.lastName as lastName7_3_,
    c.address_id as address_8_3_
from
    Customer_AUD c
inner join
    Address_AUD a
        on (
            c.address_id=a.id
            or (
                c.address_id is null
            )
            and (
                a.id is null
            )
        )
where
    c.REV<=?
    and c.REVTYPE<>?
    and (
        c.REVEND>?
        or c.REVEND is null
    )
    and a.REV<=?
    and a.country=?
    and (
        a.REVEND>?
        or a.REVEND is null
    )

-- binding parameter [1] as [INTEGER] - [1]
-- binding parameter [2] as [INTEGER] - [2]
-- binding parameter [3] as [INTEGER] - [1]
-- binding parameter [4] as [INTEGER] - [1]
-- binding parameter [5] as [VARCHAR] - [România]
-- binding parameter [6] as [INTEGER] - [1]