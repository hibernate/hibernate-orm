SELECT b.id AS id1_0_0_,
       b.name AS name2_0_0_
FROM   Batch b
WHERE  b.id = 1

-- Change batch name

UPDATE batch
SET    name = 'Proposed change request'
WHERE  id = 1