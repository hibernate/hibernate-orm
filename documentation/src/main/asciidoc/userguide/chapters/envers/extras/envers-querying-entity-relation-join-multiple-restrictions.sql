select
  c.id as id1_5_,
  c.REV as REV2_5_,
  c.REVTYPE as REVTYPE3_5_,
  c.REVEND as REVEND4_5_,
  c.created_on as created_5_5_,
  c.firstName as firstNam6_5_,
  c.lastName as lastName7_5_,
  c.address_id as address_8_5_
from
  Customer_AUD c
left outer join
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
  and (
    a.REV is null
    or a.REV<=?
    and (
      a.REVEND>?
      or a.REVEND is null
    )
  )
  and (
    a.city=?
    or a.country_id is null
  )

-- binding parameter [1] as [INTEGER] - [1]
-- binding parameter [2] as [INTEGER] - [2]
-- binding parameter [3] as [INTEGER] - [1]
-- binding parameter [4] as [INTEGER] - [1]
-- binding parameter [5] as [INTEGER] - [1]
-- binding parameter [6] as [VARCHAR] - [Cluj-Napoca]
