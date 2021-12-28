    select distinct
        p.id,
        b.author_id,
        b.id,
        b.title,
        p.first_name,
        p.last_name
    from
        person p
    left join
        book b
            on p.id=b.author_id
