SELECT
    max(order_id) + 1
FROM
    Employee
WHERE
    department_id = ?

-- binding parameter [1] as [BIGINT] - [1]

SELECT
    e.id as id1_1_0_,
    e.department_id as departme3_1_0_,
    e.username as username2_1_0_
FROM
    Employee e
WHERE
    e.department_id=?
    AND e.order_id=?

-- binding parameter [1] as [BIGINT]  - [1]
-- binding parameter [2] as [INTEGER] - [0]

SELECT
    e.id as id1_1_0_,
    e.department_id as departme3_1_0_,
    e.username as username2_1_0_
FROM
    Employee e
WHERE
    e.department_id=?
    AND e.order_id=?

-- binding parameter [1] as [BIGINT]  - [1]
-- binding parameter [2] as [INTEGER] - [1]

SELECT
    e.id as id1_1_0_,
    e.department_id as departme3_1_0_,
    e.username as username2_1_0_
FROM
    Employee e
WHERE
    e.department_id=?
    AND e.order_id=?

-- binding parameter [1] as [BIGINT]  - [1]
-- binding parameter [2] as [INTEGER] - [2]