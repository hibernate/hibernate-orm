SELECT
    p.created_on
FROM
    Person p
WHERE
    p.id = ?

-- binding parameter [1] as [BIGINT] - [1]
-- extracted value ([CREATED_ON] : [TIMESTAMP]) - [2018-11-23 10:00:00.0]

-- The created_on timestamp value is: [2018-11-23 10:00:00.0]
-- For the current time zone: [Eastern European Time], the UTC time zone offset is: [7200000]
