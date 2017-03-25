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
    ( id, companyName ) in (
        select
            id,
            companyName
        from (
        values
            ( 1,'Red Hat USA' ),
            ( 3,'Red Hat USA' ),
            ( 1,'Red Hat Europe' ),
            ( 3,'Red Hat Europe' )
        ) as HT
            (id, companyName)
    )

delete
from
    Doctor
where
    ( id, companyName ) in (
         select
            id,
            companyName
        from (
        values
            ( 1,'Red Hat USA' ),
            ( 3,'Red Hat USA' ),
            ( 1,'Red Hat Europe' ),
            ( 3,'Red Hat Europe' )
        ) as HT
            (id, companyName)
    )

delete
from
    Person
where
    ( id, companyName ) in (
        select
            id,
            companyName
        from (
        values
            ( 1,'Red Hat USA' ),
            ( 3,'Red Hat USA' ),
            ( 1,'Red Hat Europe' ),
            ( 3,'Red Hat Europe' )
        ) as HT
            (id, companyName)
    )