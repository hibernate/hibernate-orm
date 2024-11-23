
-- Sample file used to test import feature of multiline SQL script (HHH-2403).
-- Contains various SQL instructions with comments.

INSERT INTO human (id, fname, lname) VALUES (
	1,
	'John',
	'Wick'
);

INSERT INTO human (id, fname, lname) VALUES (
	2,
	'Thomas',
	'Anderson
-- follow the white rabbit
/* take the red pill */'
);

INSERT INTO human (id, fname, lname) VALUES (
	3,
	/* a comment */ 'Theodore' /* and another */,
	'Logan' -- yet a third
);

-- comment;
-- comment;

