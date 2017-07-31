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
where
    c.address_id = ?
order by
    c.lastName desc
limit ?
offset ?