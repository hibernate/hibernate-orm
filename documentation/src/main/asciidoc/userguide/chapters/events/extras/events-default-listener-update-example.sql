update
    Person
set
    createdOn=?,
    updatedOn=?,
    name=?
where
    id=?

-- binding parameter [1] as [TIMESTAMP] - [2017-06-08 19:23:48.224]
-- binding parameter [2] as [TIMESTAMP] - [2017-06-08 19:23:48.316]
-- binding parameter [3] as [VARCHAR]   - [Vlad-Alexandru Mihalcea]
-- binding parameter [4] as [BIGINT]    - [1]

update
    Book
set
    createdOn=?,
    updatedOn=?,
    author_id=?,
    title=?
where
    id=?

-- binding parameter [1] as [TIMESTAMP] - [2017-06-08 19:23:48.246]
-- binding parameter [2] as [TIMESTAMP] - [2017-06-08 19:23:48.317]
-- binding parameter [3] as [BIGINT]    - [1]
-- binding parameter [4] as [VARCHAR]   - [High-Performance Java Persistence 2nd Edition]
-- binding parameter [5] as [BIGINT]    - [1]
