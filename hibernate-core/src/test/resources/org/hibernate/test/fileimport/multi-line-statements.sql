
-- Sample file used to test import feature of multiline SQL script (HHH-2403).
-- Contains various SQL instructions with comments.

CREATE TABLE test_data (
  id    NUMBER        NOT NULL   PRIMARY KEY -- primary key
, text  VARCHAR2(100)                        /* any other data */
);

INSERT INTO test_data VALUES (1, 'sample');

DELETE
  FROM test_data;

/*
 * Data insertion...
 */
INSERT INTO test_data VALUES (2, 'Multi-line comment line 1
-- line 2''
/* line 3 */');

/* Invalid insert: INSERT INTO test_data VALUES (1, NULL); */
-- INSERT INTO test_data VALUES (1, NULL);

INSERT INTO test_data VALUES (3 /* 'third record' */, NULL /* value */); -- with NULL value
INSERT INTO test_data (id, text)
     VALUES
          (
            4 -- another record
          , NULL
          );

