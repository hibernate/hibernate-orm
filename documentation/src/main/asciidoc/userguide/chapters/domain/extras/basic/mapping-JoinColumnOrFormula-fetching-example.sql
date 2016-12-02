SELECT
    u.id as id1_1_0_,
    u.language as language3_1_0_,
    u.firstName as firstNam2_1_0_,
    u.lastName as lastName4_1_0_,
    1 as formula1_0_,
    c.id as id1_0_1_,
    c.is_default as is_defau2_0_1_,
    c.name as name3_0_1_,
    c.primaryLanguage as primaryL4_0_1_
FROM
    users u
LEFT OUTER JOIN
    countries c
        ON u.language = c.primaryLanguage
        AND 1 = c.is_default
WHERE
    u.id = ?
        
-- binding parameter [1] as [BIGINT] - [1]

SELECT
    u.id as id1_1_0_,
    u.language as language3_1_0_,
    u.firstName as firstNam2_1_0_,
    u.lastName as lastName4_1_0_,
    1 as formula1_0_,
    c.id as id1_0_1_,
    c.is_default as is_defau2_0_1_,
    c.name as name3_0_1_,
    c.primaryLanguage as primaryL4_0_1_
FROM
    users u
LEFT OUTER JOIN
    countries c
        ON u.language = c.primaryLanguage
        AND 1 = c.is_default
WHERE
    u.id = ?

-- binding parameter [1] as [BIGINT] - [2]