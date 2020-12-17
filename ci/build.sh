#! /bin/bash

goal=
if [ "$RDBMS" == "derby" ]; then
  goal="-Pdb=derby"
elif [ "$RDBMS" == "mariadb" ]; then
  goal="-Pdb=mariadb_ci"
elif [ "$RDBMS" == "postgresql" ]; then
  goal="-Pdb=pgsql_ci"
elif [ "$RDBMS" == "oracle" ]; then
  goal="-Pdb=oracle_ci"
elif [ "$RDBMS" == "db2" ]; then
  goal="-Pdb=db2_ci"
elif [ "$RDBMS" == "mssql" ]; then
  goal="-Pdb=mssql_ci"
fi

exec ./gradlew check ${goal} -Plog-test-progress=true --stacktrace