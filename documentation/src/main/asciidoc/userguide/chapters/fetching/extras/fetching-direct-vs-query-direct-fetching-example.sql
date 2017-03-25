select
    e.id as id1_1_0_,
    e.department_id as departme3_1_0_,
    e.username as username2_1_0_,
    d.id as id1_0_1_ 
from
    Employee e 
left outer join
    Department d 
        on e.department_id=d.id 
where
    e.id = 1