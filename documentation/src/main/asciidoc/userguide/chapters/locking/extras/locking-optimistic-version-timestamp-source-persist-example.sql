CALL current_timestamp()

INSERT INTO
    Person
    (firstName, lastName, version, id)
VALUES
    (?, ?, ?, ?)

-- binding parameter [1] as [VARCHAR]   - [John]
-- binding parameter [2] as [VARCHAR]   - [Doe]
-- binding parameter [3] as [TIMESTAMP] - [2017-05-18 12:03:03.808]
-- binding parameter [4] as [BIGINT]    - [1]