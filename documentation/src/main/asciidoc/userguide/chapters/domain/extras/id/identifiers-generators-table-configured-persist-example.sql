select
    tbl.product_id
from
    table_identifier tbl
where
    tbl.table_name = ?
for update

-- binding parameter [1] - [Product]

insert
into
    table_identifier
    (table_name, product_id)
values
    (?, ?)

-- binding parameter [1] - [Product]
-- binding parameter [2] - [1]

update
    table_identifier
set
    product_id= ?
where
    product_id= ?
    and table_name= ?

-- binding parameter [1] - [6]
-- binding parameter [2] - [1]

select
    tbl.product_id
from
    table_identifier tbl
where
    tbl.table_name= ? for update

update
    table_identifier
set
    product_id= ?
where
    product_id= ?
    and table_name= ?

-- binding parameter [1] - [11]
-- binding parameter [2] - [6]

insert
into
    Product
    (product_name, id)
values
    (?, ?)

-- binding parameter [1] as [VARCHAR] - [Product 1]
-- binding parameter [2] as [BIGINT]  - [1]

insert
into
    Product
    (product_name, id)
values
    (?, ?)

-- binding parameter [1] as [VARCHAR] - [Product 2]
-- binding parameter [2] as [BIGINT]  - [2]

insert
into
    Product
    (product_name, id)
values
    (?, ?)

-- binding parameter [1] as [VARCHAR] - [Product 3]
-- binding parameter [2] as [BIGINT]  - [3]
