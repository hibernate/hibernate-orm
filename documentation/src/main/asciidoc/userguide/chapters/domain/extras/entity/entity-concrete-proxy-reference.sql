select
    sc1_0.disc_col
from
    SingleBase sc1_0
where
    sc1_0.disc_col in ('SingleChild1', 'SingleSubChild1')
  and sc1_0.id=?

-- binding parameter (1:BIGINT) <- [1]
-- extracted value (1:VARCHAR) -> [SingleSubChild1]