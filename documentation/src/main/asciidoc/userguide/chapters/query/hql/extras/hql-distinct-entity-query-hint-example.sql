SELECT
    p.id as id1_1_0_,
    b.id as id1_0_1_,
    p.first_name as first_na2_1_0_,
    p.last_name as last_nam3_1_0_,
    b.author_id as author_i3_0_1_,
    b.title as title2_0_1_,
    b.author_id as author_i3_0_0__,
    b.id as id1_0_0__
FROM person p
LEFT OUTER JOIN book b ON p.id=b.author_id