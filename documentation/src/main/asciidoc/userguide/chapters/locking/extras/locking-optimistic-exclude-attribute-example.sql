-- Bob changes the Phone call count

update
    Phone 
set
    callCount = 1,
    "number" = '123-456-7890',
    version = 0
where
    id = 1
    and version = 0

-- Alice changes the Phone number

update
    Phone
set
    callCount = 0,
    "number" = '+123-456-7890',
    version = 1
where
    id = 1
    and version = 0