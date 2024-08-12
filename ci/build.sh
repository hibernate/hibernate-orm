#! /bin/bash

goal=
if [ "$RDBMS" == "h2" ]; then
  # This is the default.
  goal=""
elif [ "$RDBMS" == "hsqldb" ] || [ "$RDBMS" == "hsqldb_2_6" ]; then
  goal="-Pdb=hsqldb"
elif [ "$RDBMS" == "derby" ]; then
  goal="-Pdb=derby"
elif [ "$RDBMS" == "mysql" ] || [ "$RDBMS" == "mysql_8_0" ]; then
  goal="-Pdb=mysql_ci"
elif [ "$RDBMS" == "mariadb" ] || [ "$RDBMS" == "mariadb_10_4" ]; then
  goal="-Pdb=mariadb_ci"
elif [ "$RDBMS" == "postgresql" ] || [ "$RDBMS" == "postgresql_12" ]; then
  goal="-Pdb=pgsql_ci"
elif [ "$RDBMS" == "edb" ] || [ "$RDBMS" == "edb_12" ]; then
  goal="-Pdb=edb_ci -DdbHost=localhost:5444"
elif [ "$RDBMS" == "oracle" ]; then
  goal="-Pdb=oracle_ci"
elif [ "$RDBMS" == "oracle_xe" ] || [ "$RDBMS" == "oracle_21" ]; then
  # I have no idea why, but these tests don't seem to work on CI...
  goal="-Pdb=oracle_xe_ci"
elif [ "$RDBMS" == "oracle_atps_tls" ]; then
  echo "Managing Oracle Autonomous Database..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=autonomous&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  # I have no idea why, but these tests don't seem to work on CI...
  goal="-Pdb=oracle_cloud_autonomous_tls -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_atps" ]; then
  echo "Managing Oracle Autonomous Database..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=autonomous2&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  # I have no idea why, but these tests don't seem to work on CI...
  goal="-Pdb=oracle_cloud_autonomous -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_db19c" ]; then
  echo "Managing Oracle Database 19c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db19c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  # I have no idea why, but these tests don't seem to work on CI...
  goal="-Pdb=oracle_cloud_db19c -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_db21c" ]; then
  echo "Managing Oracle Database 21c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db21c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  # I have no idea why, but these tests don't seem to work on CI...
  goal="-Pdb=oracle_cloud_db21c -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_db23c" ]; then
  echo "Managing Oracle Database 23c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db23c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  # I have no idea why, but these tests don't seem to work on CI...
  goal="-Pdb=oracle_cloud_db23c -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "db2" ]; then
  goal="-Pdb=db2_ci"
elif [ "$RDBMS" == "db2_10_5" ]; then
  goal="-Pdb=db2"
elif [ "$RDBMS" == "mssql" ] || [ "$RDBMS" == "mssql_2017" ]; then
  goal="-Pdb=mssql_ci"
elif [ "$RDBMS" == "sybase" ]; then
  goal="-Pdb=sybase_ci"
elif [ "$RDBMS" == "sybase_jconn" ]; then
  goal="-Pdb=sybase_jconn_ci"
elif [ "$RDBMS" == "tidb" ]; then
  goal="-Pdb=tidb"
elif [ "$RDBMS" == "hana_cloud" ]; then
  goal="-Pdb=hana_cloud"
elif [ "$RDBMS" == "cockroachdb" ]; then
  goal="-Pdb=cockroachdb"
elif [ "$RDBMS" == "altibase" ]; then
  goal="-Pdb=altibase"
elif [ "$RDBMS" == "informix" ]; then
  goal="-Pdb=informix"
fi

# Only run checkstyle in the H2 build,
# so that CI jobs give a more complete report
# and developers can fix code style and non-H2 DB tests in parallel.
if [ -n "$goal" ]; then
  goal="$goal -x checkstyleMain -DPOPULATE_REMOTE=true"
fi

function logAndExec() {
  echo 1>&2 "Executing:" "${@}"
  exec "${@}"
}

logAndExec ./gradlew check ${goal} "${@}" -Plog-test-progress=true --stacktrace
