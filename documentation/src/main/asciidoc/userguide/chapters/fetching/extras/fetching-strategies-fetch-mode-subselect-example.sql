SELECT
    d.id as id1_0_
FROM
    Department d
where
    d.name like 'Department%'
    
-- Fetched 2 Departments

SELECT
    e.department_id as departme3_1_1_,
    e.id as id1_1_1_,
    e.id as id1_1_0_,
    e.department_id as departme3_1_0_,
    e.username as username2_1_0_
FROM
    Employee e
WHERE
    e.department_id in (
        SELECT
            fetchmodes0_.id
        FROM
            Department fetchmodes0_
        WHERE
            d.name like 'Department%'
    )