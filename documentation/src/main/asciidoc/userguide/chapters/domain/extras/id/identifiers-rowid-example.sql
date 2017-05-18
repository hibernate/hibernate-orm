SELECT
    p.id as id1_0_0_,
    p."name" as name2_0_0_,
    p."number" as number3_0_0_,
    p.ROWID as rowid_0_
FROM
    Product p
WHERE
    p.id = ?

-- binding parameter [1] as [BIGINT] - [1]

-- extracted value ([name2_0_0_] : [VARCHAR]) - [Mobile phone]
-- extracted value ([number3_0_0_] : [VARCHAR]) - [123-456-7890]
-- extracted ROWID value: AAAwkBAAEAAACP3AAA

UPDATE
    Product
SET
    "name" = ?,
    "number" = ?
WHERE
    ROWID = ?

-- binding parameter [1] as [VARCHAR] - [Smart phone]
-- binding parameter [2] as [VARCHAR] - [123-456-7890]
-- binding parameter [3] as ROWID     - [AAAwkBAAEAAACP3AAA]
