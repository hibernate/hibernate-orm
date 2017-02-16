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
        ( id, companyName )
    in (
        ( 1,'Red Hat USA' ),
        ( 3,'Red Hat USA' ),
        ( 1,'Red Hat Europe' ),
        ( 3,'Red Hat Europe' )
    )

delete
from
    Doctor
where
        ( id, companyName )
    in (
        ( 1,'Red Hat USA' ),
        ( 3,'Red Hat USA' ),
        ( 1,'Red Hat Europe' ),
        ( 3,'Red Hat Europe' )
    )

delete
from
    Person
where
        ( id, companyName )
    in (
        ( 1,'Red Hat USA' ),
        ( 3,'Red Hat USA' ),
        ( 1,'Red Hat Europe' ),
        ( 3,'Red Hat Europe' )
    )