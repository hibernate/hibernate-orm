delete from Engineer to_delete_
where (to_delete_.id, to_delete_.companyName) in (
    select p.id, p.companyName
    from Person p
    where p.employed = ?
);

delete from Doctor to_delete_
where (to_delete_.id, to_delete_.companyName) in (
    select p.id, p.companyName
    from Person p
    where p.employed = ?
);

delete from Person p
where p.employed = ?;
