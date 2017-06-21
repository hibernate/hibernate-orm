select
    filtersqlf0_.id as id1_0_,
    filtersqlf0_.active as active2_0_,
    filtersqlf0_.amount as amount3_0_,
    filtersqlf0_.rate as rate4_0_,
    filtersqlf0_1_.deleted as deleted1_1_
from
    account filtersqlf0_
left outer join
    account_details filtersqlf0_1_
        on filtersqlf0_.id=filtersqlf0_1_.id
where
    filtersqlf0_.active = ?
    and filtersqlf0_1_.deleted = false

-- binding parameter [1] as [BOOLEAN] - [true]
