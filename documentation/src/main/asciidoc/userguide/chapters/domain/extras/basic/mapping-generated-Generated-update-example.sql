UPDATE
    Person
SET
    firstName=?,
    lastName=?,
    middleName1=?,
    middleName2=?,
    middleName3=?,
    middleName4=?,
    middleName5=?
WHERE
    id=?

-- binding parameter [1] as [VARCHAR] - [John]
-- binding parameter [2] as [VARCHAR] - [Doe Jr]
-- binding parameter [3] as [VARCHAR] - [Flávio]
-- binding parameter [4] as [VARCHAR] - [André]
-- binding parameter [5] as [VARCHAR] - [Frederico]
-- binding parameter [6] as [VARCHAR] - [Rúben]
-- binding parameter [7] as [VARCHAR] - [Artur]
-- binding parameter [8] as [BIGINT]  - [1]

SELECT
    p.fullName as fullName3_0_
FROM
    Person p
WHERE
    p.id=?

-- binding parameter [1] as [BIGINT] - [1]
-- extracted value ([fullName3_0_] : [VARCHAR]) - [John Flávio André Frederico Rúben Artur Doe Jr]