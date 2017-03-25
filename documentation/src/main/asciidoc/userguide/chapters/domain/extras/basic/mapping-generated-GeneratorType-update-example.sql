UPDATE Person 
SET
    createdBy = ?,
    firstName = ?,
    lastName = ?,
    updatedBy = ? 
WHERE
    id = ?

-- binding parameter [1] as [VARCHAR] - [Alice]
-- binding parameter [2] as [VARCHAR] - [Mr. John]
-- binding parameter [3] as [VARCHAR] - [Doe]
-- binding parameter [4] as [VARCHAR] - [Bob]
-- binding parameter [5] as [BIGINT]  - [1]