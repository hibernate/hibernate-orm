select
    c.id,
    c.duration,
    c.phone_id,
    c.call_timestamp
from
    phone_call c
join
    Phone p
        on p.id=c.phone_id
where
    p.phone_number='123-456-7890'
