SELECT
    d.id as id1_0_
FROM
    Department d
INNER JOIN
    Employee employees1_
    ON d.id=employees1_.department_id

SELECT
    e.department_id as departme3_1_1_,
    e.id as id1_1_1_,
    e.id as id1_1_0_,
    e.department_id as departme3_1_0_,
    e.name as name2_1_0_
FROM
    Employee e
WHERE
    e.department_id IN (
        0, 2, 3, 4, 5
    )

SELECT
    e.department_id as departme3_1_1_,
    e.id as id1_1_1_,
    e.id as id1_1_0_,
    e.department_id as departme3_1_0_,
    e.name as name2_1_0_
FROM
    Employee e
WHERE
    e.department_id IN (
        6, 7, 8, 9, 1
    )