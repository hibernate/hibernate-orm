DEBUG SQL:92 -
    insert
    into
        Product
        (bitSet, id)
    values
        (?, ?)

TRACE BasicBinder:65 - binding parameter [1] as [VARCHAR] - [{0, 65, 128, 129}]
TRACE BasicBinder:65 - binding parameter [2] as [INTEGER] - [1]

DEBUG SQL:92 -
    select
        bitsettype0_.id as id1_0_0_,
        bitsettype0_.bitSet as bitSet2_0_0_
    from
        Product bitsettype0_
    where
        bitsettype0_.id=?

TRACE BasicBinder:65 - binding parameter [1] as [INTEGER] - [1]
TRACE BasicExtractor:61 - extracted value ([bitSet2_0_0_] : [VARCHAR]) - [{0, 65, 128, 129}]