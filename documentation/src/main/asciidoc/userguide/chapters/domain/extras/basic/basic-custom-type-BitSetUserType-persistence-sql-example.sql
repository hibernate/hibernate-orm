DEBUG SQL:92 -
    insert
    into
        Product
        (bitSet, id)
    values
        (?, ?)

DEBUG BitSetUserType:71 - Binding 1,10,11 to parameter 1
TRACE BasicBinder:65 - binding parameter [2] as [INTEGER] - [1]

DEBUG SQL:92 -
    select
        bitsetuser0_.id as id1_0_0_,
        bitsetuser0_.bitSet as bitSet2_0_0_
    from
        Product bitsetuser0_
    where
        bitsetuser0_.id=?

TRACE BasicBinder:65 - binding parameter [1] as [INTEGER] - [1]
DEBUG BitSetUserType:56 - Result set column bitSet2_0_0_ value is 1,10,11