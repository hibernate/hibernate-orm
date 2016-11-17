SELECT
    d.id as id1_0_0_,
    e.department_id as departme3_1_1_,
    e.id as id1_1_1_,
    e.id as id1_1_2_,
    e.department_id as departme3_1_2_,
    e.username as username2_1_2_
FROM
    Department d
LEFT OUTER JOIN
    Employee e
        on d.id = e.department_id
WHERE
    d.id = 1

-- Fetched department: 1