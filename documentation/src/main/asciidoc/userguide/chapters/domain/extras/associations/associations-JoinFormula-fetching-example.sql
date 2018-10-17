-- Fetch User entities

SELECT
    u.id as id1_1_0_,
    u.firstName as firstNam2_1_0_,
    u.lastName as lastName3_1_0_,
    u.phoneNumber as phoneNum4_1_0_,
    REGEXP_REPLACE(u.phoneNumber, '\+(\d+)-.*', '\1')::int as formula1_0_,
    c.id as id1_0_1_,
    c.name as name2_0_1_
FROM
    users u
LEFT OUTER JOIN
    countries c
        ON REGEXP_REPLACE(u.phoneNumber, '\+(\d+)-.*', '\1')::int = c.id
WHERE
    u.id=?

-- binding parameter [1] as [BIGINT] - [1]

SELECT
    u.id as id1_1_0_,
    u.firstName as firstNam2_1_0_,
    u.lastName as lastName3_1_0_,
    u.phoneNumber as phoneNum4_1_0_,
    REGEXP_REPLACE(u.phoneNumber, '\+(\d+)-.*', '\1')::int as formula1_0_,
    c.id as id1_0_1_,
    c.name as name2_0_1_
FROM
    users u
LEFT OUTER JOIN
    countries c
        ON REGEXP_REPLACE(u.phoneNumber, '\+(\d+)-.*', '\1')::int = c.id
WHERE
    u.id=?

-- binding parameter [1] as [BIGINT] - [2]