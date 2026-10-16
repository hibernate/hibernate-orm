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
        ( 2,'Red Hat USA' ),
        ( 4,'Red Hat USA' ),
        ( 6,'Red Hat Europe' ),
        ( 8,'Red Hat Europe' )
    )

delete
from
    Doctor
where
        ( id, companyName )
    in (
        ( 2,'Red Hat USA' ),
        ( 4,'Red Hat USA' ),
        ( 6,'Red Hat Europe' ),
        ( 8,'Red Hat Europe' )
    )

delete
from
    Person
where
        ( id, companyName )
    in (
        ( 2,'Red Hat USA' ),
        ( 4,'Red Hat USA' ),
        ( 6,'Red Hat Europe' ),
        ( 8,'Red Hat Europe' )
    )