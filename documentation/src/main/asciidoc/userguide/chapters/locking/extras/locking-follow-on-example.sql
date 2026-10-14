-- Initial selection (aliases and column order omitted for clarity)
select id, title, version
from GuideBook
order by id;

-- Grouped acquisition for the selected identifiers
select id, title, version
from GuideBook
where id in (?, ?)
for update;
