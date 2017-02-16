create temporary table
    HT_Person
(
    id int4 not null,
    companyName varchar(255) not null
)

insert
into
    HT_Person
    select
        p.id as id,
        p.companyName as companyName
    from
        Person p
    where
        p.employed = ?

delete
from
    Engineer
where
    (
        id, companyName
    ) IN (
        select
            id,
            companyName
        from
            HT_Person
    )

delete
from
    Doctor
where
    (
        id, companyName
    ) IN (
        select
            id,
            companyName
        from
            HT_Person
    )

delete
from
    Person
where
    (
        id, companyName
    ) IN (
        select
            id,
            companyName
        from
            HT_Person
    )