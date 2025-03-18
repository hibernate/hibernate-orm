insert
into
    Customer
    (created_on, firstName, lastName, id)
values
    (?, ?, ?, ?)

-- binding parameter [1] as [TIMESTAMP] - [Mon Jul 24 17:21:32 EEST 2017]
-- binding parameter [2] as [VARCHAR]   - [John]
-- binding parameter [3] as [VARCHAR]   - [Doe]
-- binding parameter [4] as [BIGINT]    - [1]

insert
into
    REVINFO
    (REV, REVTSTMP)
values
    (?, ?)

-- binding parameter [1] as [BIGINT]    - [1]
-- binding parameter [2] as [BIGINT]    - [1500906092803]

insert
into
    Customer_AUD
    (REVTYPE, created_on, firstName, lastName, id, REV)
values
    (?, ?, ?, ?, ?, ?)

-- binding parameter [1] as [INTEGER]   - [0]
-- binding parameter [2] as [TIMESTAMP] - [Mon Jul 24 17:21:32 EEST 2017]
-- binding parameter [3] as [VARCHAR]   - [John]
-- binding parameter [4] as [VARCHAR]   - [Doe]
-- binding parameter [5] as [BIGINT]    - [1]
-- binding parameter [6] as [INTEGER]   - [1]
