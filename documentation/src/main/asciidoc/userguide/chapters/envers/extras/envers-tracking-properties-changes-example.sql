update
    Customer 
set
    created_on = ?,
    firstName = ?,
    lastName = ? 
where
    id = ?

-- binding parameter [1] as [TIMESTAMP] - [2017-07-31 15:58:20.342]
-- binding parameter [2] as [VARCHAR]   - [John]
-- binding parameter [3] as [VARCHAR]   - [Doe Jr.]
-- binding parameter [4] as [BIGINT]    - [1]

insert
into
    REVINFO
    (REV, REVTSTMP)
values
    (null, ?)

-- binding parameter [1] as [BIGINT] - [1501505900439]

insert
into
    Customer_AUD
    (REVTYPE, created_on, createdOn_MOD, firstName, firstName_MOD, lastName, lastName_MOD, id, REV)
values
    (?, ?, ?, ?, ?, ?, ?, ?, ?)

-- binding parameter [1] as [INTEGER]   - [1]
-- binding parameter [2] as [TIMESTAMP] - [2017-07-31 15:58:20.342]
-- binding parameter [3] as [BOOLEAN]   - [false]
-- binding parameter [4] as [VARCHAR]   - [John]
-- binding parameter [5] as [BOOLEAN]   - [false]
-- binding parameter [6] as [VARCHAR]   - [Doe Jr.]
-- binding parameter [7] as [BOOLEAN]   - [true]
-- binding parameter [8] as [BIGINT]    - [1]
-- binding parameter [9] as [INTEGER]   - [2]