select
    p.id as id1_2_0_, e.id as id1_1_1_, d.id as id1_0_2_,
    e.accessLevel as accessLe2_1_1_,
    e.department_id as departme5_1_1_,
    decrypt( 'AES', '00', e.pswd  ) as pswd3_1_1_,
    e.username as username4_1_1_,
    p_e.projects_id as projects1_3_0__,
    p_e.employees_id as employee2_3_0__
from
    Project p
inner join
    Project_Employee p_e
        on p.id=p_e.projects_id
inner join
    Employee e
        on p_e.employees_id=e.id
inner join
    Department d
        on e.department_id=d.id
where
    p.id = ?

-- binding parameter [1] as [BIGINT] - [1]
