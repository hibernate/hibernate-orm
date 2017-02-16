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
    ( id = 1 and companyName = 'Red Hat USA' )
or  ( id = 3 and companyName = 'Red Hat USA' )
or  ( id = 1 and companyName = 'Red Hat Europe' )
or  ( id = 3 and companyName = 'Red Hat Europe' )

delete
from
    Doctor
where
    ( id = 1 and companyName = 'Red Hat USA' )
or  ( id = 3 and companyName = 'Red Hat USA' )
or  ( id = 1 and companyName = 'Red Hat Europe' )
or  ( id = 3 and companyName = 'Red Hat Europe' )

delete
from
    Person
where
    ( id = 1 and companyName = 'Red Hat USA' )
or  ( id = 3 and companyName = 'Red Hat USA' )
or  ( id = 1 and companyName = 'Red Hat Europe' )
or  ( id = 3 and companyName = 'Red Hat Europe' )