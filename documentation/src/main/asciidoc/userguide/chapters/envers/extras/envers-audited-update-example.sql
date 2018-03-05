update
    Customer
set
    created_on=?,
    firstName=?,
    lastName=?
where
    id=?

-- binding parameter [1] as [TIMESTAMP] - [2017-07-24 17:21:32.757]
-- binding parameter [2] as [VARCHAR]   - [John]
-- binding parameter [3] as [VARCHAR]   - [Doe Jr.]
-- binding parameter [4] as [BIGINT]    - [1]

insert
into
    REVINFO
    (REV, REVTSTMP)
values
    (?, ?)

-- binding parameter [1] as [BIGINT]    - [2]
-- binding parameter [2] as [BIGINT]    - [1500906092853]

insert
into
    Customer_AUD
    (REVTYPE, created_on, firstName, lastName, id, REV)
values
    (?, ?, ?, ?, ?, ?)

-- binding parameter [1] as [INTEGER]   - [1]
-- binding parameter [2] as [TIMESTAMP] - [2017-07-24 17:21:32.757]
-- binding parameter [3] as [VARCHAR]   - [John]
-- binding parameter [4] as [VARCHAR]   - [Doe Jr.]
-- binding parameter [5] as [BIGINT]    - [1]
-- binding parameter [6] as [INTEGER]   - [2]
