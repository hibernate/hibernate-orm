select
    sp1_0.id,
    sp1_0.single_id,
    s1_0.disc_col
from
    SingleParent sp1_0
        left join
    SingleBase s1_0
    on s1_0.id=sp1_0.single_id
where
    sp1_0.id=?

-- binding parameter (1:BIGINT) <- [1]
-- extracted value (2:BIGINT) -> [1]
-- extracted value (3:VARCHAR) -> [SingleSubChild1]