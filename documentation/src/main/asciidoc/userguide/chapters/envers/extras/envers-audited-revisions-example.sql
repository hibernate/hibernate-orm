select
    c.REV as col_0_0_ 
from
    Customer_AUD c 
cross join
    REVINFO r 
where
    c.id = ?
    and c.REV = r.REV
order by
    c.REV asc

-- binding parameter [1] as [BIGINT] - [1]