-- Illustrative SQL for ten departments and collection batch size five.
select d.id
from Department d
order by d.id;

select e.department_id, e.id, e.name
from Employee e
where e.department_id in (?, ?, ?, ?, ?);

select e.department_id, e.id, e.name
from Employee e
where e.department_id in (?, ?, ?, ?, ?);
