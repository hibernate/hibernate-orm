UPDATE Bid SET
    cents = ?,
    updated_by = ?,
    updated_on = ?
where
    id = ?

-- binding parameter [1] as [BIGINT]    - [16000]
-- binding parameter [2] as [VARCHAR]   - [John Doe Jr.]
-- binding parameter [3] as [TIMESTAMP] - [Tue Apr 18 17:49:24 EEST 2017]
-- binding parameter [4] as [BIGINT]    - [1]