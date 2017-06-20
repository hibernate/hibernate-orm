insert
into
    Person
    (createdOn, updatedOn, name, id)
values
    (?, ?, ?, ?)

-- binding parameter [1] as [TIMESTAMP] - [2017-06-08 19:23:48.224]
-- binding parameter [2] as [TIMESTAMP] - [null]
-- binding parameter [3] as [VARCHAR]   - [Vlad Mihalcea]
-- binding parameter [4] as [BIGINT]    - [1]

insert
into
    Book
    (createdOn, updatedOn, author_id, title, id)
values
    (?, ?, ?, ?, ?)

-- binding parameter [1] as [TIMESTAMP] - [2017-06-08 19:23:48.246]
-- binding parameter [2] as [TIMESTAMP] - [null]
-- binding parameter [3] as [BIGINT]    - [1]
-- binding parameter [4] as [VARCHAR]   - [High-Performance Java Persistence]
-- binding parameter [5] as [BIGINT]    - [1]