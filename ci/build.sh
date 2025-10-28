#! /bin/bash

goal=
if [ "$RDBMS" == "h2" ] || [ "$RDBMS" == "" ]; then
  # This is the default.
  goal="preVerifyRelease"
  # Settings needed for `preVerifyRelease` execution - for asciidoctor doc rendering
	export GRADLE_OPTS=-Dorg.gradle.jvmargs='-Dlog4j2.disableJmx -Xmx4g -XX:MaxMetaspaceSize=768m -XX:+HeapDumpOnOutOfMemoryError -Duser.language=en -Duser.country=US -Duser.timezone=UTC -Dfile.encoding=UTF-8'
elif [ "$RDBMS" == "hsqldb" ] || [ "$RDBMS" == "hsqldb_2_6" ]; then
  goal="-Pdb=hsqldb"
elif [ "$RDBMS" == "mysql" ] || [ "$RDBMS" == "mysql_8_0" ]; then
  goal="-Pdb=mysql_ci"
elif [ "$RDBMS" == "mariadb" ] || [ "$RDBMS" == "mariadb_10_6" ]; then
  goal="-Pdb=mariadb_ci"
elif [ "$RDBMS" == "postgresql" ] || [ "$RDBMS" == "postgresql_13" ]; then
  goal="-Pdb=pgsql_ci"
elif [ "$RDBMS" == "gaussdb"  ]; then
  goal="-Pdb=gaussdb -DdbHost=localhost:8000"
elif [ "$RDBMS" == "edb" ] || [ "$RDBMS" == "edb_13" ]; then
  goal="-Pdb=edb_ci -DdbHost=localhost:5444"
elif [ "$RDBMS" == "oracle" ]; then
  goal="-Pdb=oracle_ci"
elif [ "$RDBMS" == "oracle_xe" ] || [ "$RDBMS" == "oracle_21" ]; then
  goal="-Pdb=oracle_xe_ci"
elif [ "$RDBMS" == "oracle_atps_tls" ]; then
  echo "Managing Oracle Autonomous Database..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=autonomous&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_autonomous_tls -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_atps" ]; then
  echo "Managing Oracle Autonomous Database..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=autonomous2&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_autonomous -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_db19c" ]; then
  echo "Managing Oracle Database 19c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db19c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_db19c -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_db21c" ]; then
  echo "Managing Oracle Database 21c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db21c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_db21c -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
elif [ "$RDBMS" == "oracle_db23c" ]; then
  echo "Managing Oracle Database 23c..."
  export INFO=$(curl -s -k -L -X GET "https://api.atlas-controller.oraclecloud.com/ords/atlas/admin/database?type=db23c&hostname=`hostname`" -H 'accept: application/json')
  export HOST=$(echo $INFO | jq -r '.database' | jq -r '.host')
  export SERVICE=$(echo $INFO | jq -r '.database' | jq -r '.service')
  goal="-Pdb=oracle_cloud_db23c -DrunID=$RUNID -DdbHost=$HOST -DdbService=$SERVICE"
# OTP
elif [ "$RDBMS" == "autonomous-transaction-processing-serverless" ] || [ "$RDBMS" == "base-database-service-19c" ] || [ "$RDBMS" == "base-database-service-21c" ] || [ "$RDBMS" == "base-database-service-23ai" ]; then
  echo "Managing OTP Database..."
  goal="-Pdb=oracle_test_pilot_database -DrunID=$RUNID -DdbPassword=$TESTPILOT_PASSWORD -DdbConnectionStringSuffix=$TESTPILOT_CONNECTION_STRING_SUFFIX"
elif [ "$RDBMS" == "db2" ] || [ "$RDBMS" == "db2_11_5" ]; then
  goal="-Pdb=db2_ci"
elif [ "$RDBMS" == "mssql" ] || [ "$RDBMS" == "mssql_2017" ]; then
  goal="-Pdb=mssql_ci"
# Exclude some Sybase tests on CI because they use `xmltable` function which has a memory leak on the DB version in CI
elif [ "$RDBMS" == "sybase" ]; then
  goal="-Pdb=sybase_ci -PexcludeTests=**.GenerateSeriesTest*"
elif [ "$RDBMS" == "sybase_jconn" ]; then
  goal="-Pdb=sybase_jconn_ci -PexcludeTests=**.GenerateSeriesTest*"
elif [ "$RDBMS" == "teradata" ]; then 
  goal="-Pdb=teradata"
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
else
  echo "Invalid value for RDBMS: $RDBMS"
  exit 1
fi

function logAndExec() {
  echo 1>&2 "Executing:" "${@}"
  exec "${@}"
}

logAndExec ./gradlew ciCheck ${goal} "${@}" -Plog-test-progress=true --stacktrace
