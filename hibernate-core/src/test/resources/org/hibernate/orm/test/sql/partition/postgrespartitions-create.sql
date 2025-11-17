create table p1 partition of pgparts for values from (0) to (1000);
create table p2 partition of pgparts for values from (1001) to (2000);