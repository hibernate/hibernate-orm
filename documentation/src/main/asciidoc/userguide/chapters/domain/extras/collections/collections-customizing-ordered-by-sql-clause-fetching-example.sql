select
    a.person_id as person_i4_0_0_,
    a.id as id1_0_0_,
    a.content as content2_0_1_,
    a.name as name3_0_1_,
    a.person_id as person_i4_0_1_ 
from
    Article a 
where
    a.person_id = ?
order by
    CHAR_LENGTH(a.name) desc