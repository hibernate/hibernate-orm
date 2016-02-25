select
    e.id as id1_0_,
    e.location as location2_0_,
    e.name as name3_0_ 
from Event e 
where st_within(e.location, 'POLYGON ((1 1, 20 1, 20 20, 1 20, 1 1))') = true