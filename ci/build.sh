#! /bin/bash

goal=
if [ "$RDBMS" == "derby" ]; then
  goal="-Pdb=derby"
elif [ "$RDBMS" == "hsqldb" ]; then
  goal="-Pdb=hsqldb"
elif [ "$RDBMS" == "mysql8" ]; then
  goal="-Pdb=mysql_ci"
elif [ "$RDBMS" == "mysql" ]; then
  goal="-Pdb=mysql_ci"
elif [ "$RDBMS" == "mariadb" ]; then
  goal="-Pdb=mariadb_ci"
elif [ "$RDBMS" == "postgresql_9_5" ]; then
  goal="-Pdb=pgsql_ci"
elif [ "$RDBMS" == "postgresql_13" ]; then
  goal="-Pdb=pgsql_ci"
elif [ "$RDBMS" == "oracle" ]; then
  # I have no idea why, but these tests don't work on GH Actions
  goal="-Pdb=oracle_ci -PexcludeTests=**.LockTest.testQueryTimeout*"
elif [ "$RDBMS" == "db2" ]; then
  goal="-Pdb=db2_ci"
elif [ "$RDBMS" == "mssql" ]; then
  goal="-Pdb=mssql_ci"
elif [ "$RDBMS" == "hana" ]; then
  goal="-Pdb=hana_ci"
elif [ "$RDBMS" == "sybase" ]; then
  goal="-Pdb=sybase_ci"
fi

exec ./gradlew check ${goal} -Plog-test-progress=true --stacktrace
