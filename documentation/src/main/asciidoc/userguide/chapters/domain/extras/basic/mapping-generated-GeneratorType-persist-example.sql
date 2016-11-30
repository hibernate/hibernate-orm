INSERT INTO Person
(
    createdBy,
    firstName,
    lastName,
    updatedBy,
    id
)
VALUES
(?, ?, ?, ?, ?)

-- binding parameter [1] as [VARCHAR] - [Alice]
-- binding parameter [2] as [VARCHAR] - [John]
-- binding parameter [3] as [VARCHAR] - [Doe]
-- binding parameter [4] as [VARCHAR] - [Alice]
-- binding parameter [5] as [BIGINT]  - [1]