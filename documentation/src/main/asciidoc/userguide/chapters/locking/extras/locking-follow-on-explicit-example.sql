SELECT *
FROM (
    SELECT p.id as id1_0_, p."name" as name2_0_
    FROM Person p
)
WHERE rownum <= 10
FOR UPDATE