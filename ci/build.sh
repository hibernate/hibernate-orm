#! /bin/bash

goal=
if [ "$RDBMS" == "h2" ]; then
  # This is the default.
  goal=""
elif [ "$RDBMS" == "derby" ]; then
  goal="-Pdb=derby"
elif [ "$RDBMS" == "edb" ]; then
  goal="-Pdb=edb_ci -DdbHost=localhost:5444"
elif [ "$RDBMS" == "hsqldb" ]; then
  goal="-Pdb=hsqldb"
elif [ "$RDBMS" == "mysql8" ]; then
  goal="-Pdb=mysql_ci"
elif [ "$RDBMS" == "mysql" ]; then
  goal="-Pdb=mysql_ci"
elif [ "$RDBMS" == "mariadb" ]; then
  goal="-Pdb=mariadb_ci"
elif [ "$RDBMS" == "postgresql" ]; then
  goal="-Pdb=pgsql_ci"
elif [ "$RDBMS" == "postgresql_14" ]; then
  goal="-Pdb=pgsql_ci"
elif [ "$RDBMS" == "oracle" ]; then
  # I have no idea why, but these tests don't work on GH Actions
  # yrodiere: Apparently those have been disabled on Jenkins as well...
  goal="-Pdb=oracle_ci -PexcludeTests=**.LockTest.testQueryTimeout*"
elif [ "$RDBMS" == "oracle_ee" ]; then
  goal="-Pdb=oracle_jenkins"
elif [ "$RDBMS" == "db2" ]; then
  goal="-Pdb=db2_ci"
elif [ "$RDBMS" == "mssql" ]; then
  goal="-Pdb=mssql_ci"
elif [ "$RDBMS" == "hana" ]; then
  goal="-Pdb=hana_ci"
elif [ "$RDBMS" == "hana_jenkins" ]; then
  goal="-Pdb=hana_jenkins"
elif [ "$RDBMS" == "sybase" ]; then
  goal="-Pdb=sybase_ci"
elif [ "$RDBMS" == "tidb" ]; then
  goal="-Pdb=tidb"
elif [ "$RDBMS" == "cockroachdb" ]; then
  goal="-Pdb=cockroachdb"
fi

# Only run checkstyle in the H2 build,
# so that CI jobs give a more complete report
# and developers can fix code style and non-H2 DB tests in parallel.
if [ -n "$goal" ]; then
  goal="$goal -x checkstyleMain"
fi

function logAndExec() {
  echo 1>&2 "Executing:" "${@}"
  exec "${@}"
}

# Clean by default otherwise the PackagedEntityManager tests fail on a node that previously ran a different DB
logAndExec ./gradlew clean check ${goal} "${@}" -Plog-test-progress=true --stacktrace
