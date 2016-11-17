SELECT
    d.id as id1_0_
FROM
    Department d

-- Fetched 2 Departments

SELECT
    e.department_id as departme3_1_0_,
    e.id as id1_1_0_,
    e.id as id1_1_1_,
    e.department_id as departme3_1_1_,
    e.username as username2_1_1_
FROM
    Employee e
WHERE
    e.department_id = 1

SELECT
    e.department_id as departme3_1_0_,
    e.id as id1_1_0_,
    e.id as id1_1_1_,
    e.department_id as departme3_1_1_,
    e.username as username2_1_1_
FROM
    Employee e
WHERE
    e.department_id = 2
