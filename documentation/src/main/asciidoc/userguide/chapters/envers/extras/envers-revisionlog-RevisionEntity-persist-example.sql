insert
into
    Customer
    (created_on, firstName, lastName, id)
values
    (?, ?, ?, ?)

-- binding parameter [1] as [TIMESTAMP] - [Thu Jul 27 15:45:00 EEST 2017]
-- binding parameter [2] as [VARCHAR]   - [John]
-- binding parameter [3] as [VARCHAR]   - [Doe]
-- binding parameter [4] as [BIGINT]    - [1]

insert
into
    CUSTOM_REV_INFO
    (timestamp, username, id)
values
    (?, ?, ?)

-- binding parameter [1] as [BIGINT]  - [1501159500888]
-- binding parameter [2] as [VARCHAR] - [Vlad Mihalcea]
-- binding parameter [3] as [INTEGER] - [1]

insert
into
    Customer_AUD
    (REVTYPE, created_on, firstName, lastName, id, REV)
values
    (?, ?, ?, ?, ?, ?)

-- binding parameter [1] as [INTEGER]   - [0]
-- binding parameter [2] as [TIMESTAMP] - [Thu Jul 27 15:45:00 EEST 2017]
-- binding parameter [3] as [VARCHAR]   - [John]
-- binding parameter [4] as [VARCHAR]   - [Doe]
-- binding parameter [5] as [BIGINT]    - [1]
-- binding parameter [6] as [INTEGER]   - [1]
