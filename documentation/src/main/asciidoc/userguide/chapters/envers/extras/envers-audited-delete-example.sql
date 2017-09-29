delete
from
    Customer
where
    id = ?

-- binding parameter [1] as [BIGINT]    - [1]

insert
into
    REVINFO
    (REV, REVTSTMP)
values
    (?, ?)

-- binding parameter [1] as [BIGINT]    - [3]
-- binding parameter [2] as [BIGINT]    - [1500906092876]

insert
into
    Customer_AUD
    (REVTYPE, created_on, firstName, lastName, id, REV)
values
    (?, ?, ?, ?, ?, ?)

-- binding parameter [1] as [INTEGER]   - [2]
-- binding parameter [2] as [TIMESTAMP] - [null]
-- binding parameter [3] as [VARCHAR]   - [null]
-- binding parameter [4] as [VARCHAR]   - [null]
-- binding parameter [5] as [BIGINT]    - [1]
-- binding parameter [6] as [INTEGER]   - [3]