SELECT
    id 
FROM
    Person 
WHERE
    id = 1
    
SELECT
    id 
FROM
    Phone 
WHERE
    id = 1

UPDATE
    Person 
SET
    name = 'John Doe Sr.'
WHERE
    id = 1

UPDATE
    Phone 
SET
    "number" = '(01) 123-456-7890',
    owner_id = 1 
WHERE
    id = 1
