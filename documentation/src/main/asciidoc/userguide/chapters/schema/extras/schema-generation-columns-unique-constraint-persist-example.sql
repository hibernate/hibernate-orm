insert
into
    author
    (first_name, last_name, id)
values
    (?, ?, ?)

-- binding parameter [1] as [VARCHAR] - [Vlad]
-- binding parameter [2] as [VARCHAR] - [Mihalcea]
-- binding parameter [3] as [BIGINT]  - [1]

insert
into
    book
    (author_id, title, id)
values
    (?, ?, ?)

-- binding parameter [1] as [BIGINT]  - [1]
-- binding parameter [2] as [VARCHAR] - [High-Performance Java Persistence]
-- binding parameter [3] as [BIGINT]  - [2]

insert
into
    book
    (author_id, title, id)
values
    (?, ?, ?)

-- binding parameter [1] as [BIGINT]  - [1]
-- binding parameter [2] as [VARCHAR] - [High-Performance Java Persistence]
-- binding parameter [3] as [BIGINT]  - [3]

-- SQL Error: 23505, SQLState: 23505
-- Unique index or primary key violation: "UK_BOOK_TITLE_AUTHOR_INDEX_1 ON PUBLIC.BOOK(TITLE, AUTHOR_ID) VALUES ( /* key:1 */ 3, 'High-Performance Java Persistence', 1)";