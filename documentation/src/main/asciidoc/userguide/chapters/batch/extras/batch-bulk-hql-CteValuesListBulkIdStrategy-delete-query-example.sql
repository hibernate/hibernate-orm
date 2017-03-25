select
    p.id as id,
    p.companyName as companyName
from
    Person p
where
    p.employed = ?

with HT_Person (id,companyName ) as (
    select id, companyName
    from (
    values
        (?, ?),
        (?, ?),
        (?, ?),
        (?, ?)
    ) as HT (id, companyName) )
delete
from
    Engineer
where
    ( id, companyName ) in (
        select
            id, companyName
        from
            HT_Person
    )

with HT_Person (id,companyName ) as (
    select id, companyName
    from (
    values
        (?, ?),
        (?, ?),
        (?, ?),
        (?, ?)
    ) as HT (id, companyName) )
delete
from
    Doctor
where
    ( id, companyName ) in (
        select
            id, companyName
        from
            HT_Person
    )


with HT_Person (id,companyName ) as (
    select id, companyName
    from (
    values
        (?, ?),
        (?, ?),
        (?, ?),
        (?, ?)
    ) as HT (id, companyName) )
delete
from
    Person
where
    ( id, companyName ) in (
        select
            id, companyName
        from
            HT_Person
    )