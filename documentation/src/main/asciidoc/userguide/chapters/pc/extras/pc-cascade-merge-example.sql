SELECT
    p.id as id1_0_1_,
    p.name as name2_0_1_,
    ph.owner_id as owner_id3_1_3_,
    ph.id as id1_1_3_,
    ph.id as id1_1_0_,
    ph."number" as number2_1_0_,
    ph.owner_id as owner_id3_1_0_ 
FROM
    Person p 
LEFT OUTER JOIN
    Phone ph 
        on p.id=ph.owner_id 
WHERE
    p.id = 1