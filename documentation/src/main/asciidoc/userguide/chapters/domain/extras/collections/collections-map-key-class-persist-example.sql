insert into person (id) values (?)

-- binding parameter [1] as [BIGINT] - [1]

insert into call_register(
    person_id,
    country_code,
    operator_code,
    subscriber_code,
    call_register
)
values
    (?, ?, ?, ?, ?)

-- binding parameter [1] as [BIGINT]  - [1]
-- binding parameter [2] as [VARCHAR] - [01]
-- binding parameter [3] as [VARCHAR] - [234]
-- binding parameter [4] as [VARCHAR] - [789]
-- binding parameter [5] as [INTEGER] - [102]

insert into call_register(
    person_id,
    country_code,
    operator_code,
    subscriber_code,
    call_register
)
values
    (?, ?, ?, ?, ?)

-- binding parameter [1] as [BIGINT]  - [1]
-- binding parameter [2] as [VARCHAR] - [01]
-- binding parameter [3] as [VARCHAR] - [234]
-- binding parameter [4] as [VARCHAR] - [567]
-- binding parameter [5] as [INTEGER] - [101]